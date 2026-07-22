package com.example.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.MessageService;
import com.example.backend.constant.SalaryConstants;
import com.example.backend.dto.SalaryCalculationResult;
import com.example.backend.dto.SalaryCalculationResult.SkippedEmployee;
import com.example.backend.dto.attendance.WorkHoursResult;
import com.example.backend.entity.Employee;
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.entity.Position;
import com.example.backend.entity.Qualification;
import com.example.backend.entity.SalaryResult;
import com.example.backend.entity.SkillGrade;
import com.example.backend.exception.SalaryCalculationException;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.PositionRepository;
import com.example.backend.repository.QualificationRepository;
import com.example.backend.repository.SalaryResultRepository;
import com.example.backend.repository.SkillGradeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 給与計算のドメインロジックを提供するサービス。
 * Spring Batchは使用せず、通常のJavaサービスとして実装する（手動実行を想定）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalaryCalculationService {

    private final EmployeeService employeeService;
    private final WorkHoursCalculationService workHoursCalculationService;
    private final SkillGradeRepository skillGradeRepository;
    private final PositionRepository positionRepository;
    private final QualificationRepository qualificationRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
    private final SalaryResultRepository salaryResultRepository;
    private final MessageService messageService;

    /**
     * 対象年月の給与を全対象社員分計算する。
     * 既にsalary_resultへ登録済みの社員は対象から自動的に除外されるため、
     * 初回実行・再実行（補完実行）のどちらでも同じ呼び出しで良い。
     */
    public SalaryCalculationResult calculateAll(int targetYear, int targetMonth) {
        YearMonth targetYearMonth = YearMonth.of(targetYear, targetMonth);
        log.info("給与計算処理を開始します。対象年月={}年{}月", targetYear, targetMonth);

        List<Long> employeeIds = employeeService.findEligibleEmployeeIds(targetYearMonth);
        int processedCount = 0;
        List<SkippedEmployee> skipped = new ArrayList<>();

        for (Long employeeId : employeeIds) {
            try {
                calculateOne(employeeId, targetYearMonth);
                processedCount++;
            } catch (SalaryCalculationException e) {
                skipped.add(new SkippedEmployee(employeeId, e.getMessage()));
                log.warn("給与計算をスキップしました。employeeId={}, 理由={}", employeeId, e.getMessage());
            }
        }

        log.info("給与計算処理が正常終了しました。処理件数={}, スキップ件数={}", processedCount, skipped.size());
        return new SalaryCalculationResult(processedCount, skipped.size(), skipped);
    }

    /** 対象社員1名分の給与計算を行う（1社員1トランザクション）。 */
    @Transactional
    public void calculateOne(Long employeeId, YearMonth targetYearMonth) {
        LocalDate targetMonthStart = targetYearMonth.atDay(1);
        LocalDate targetMonthEnd = targetYearMonth.atEndOfMonth();


        // 1. 稼働時間・残業時間の算出
        WorkHoursResult workHours = workHoursCalculationService.calculateWorkHours(employeeId, targetMonthStart, targetMonthEnd);

        // 2. 対象年月時点で有効なemployeeレコードを1件取得（部署・職能資格・役職をまとめて取得）
        Employee employee = employeeService.findEffectiveEmployeeAt(employeeId, targetMonthEnd);
        if (employee == null) {
            throw new SalaryCalculationException(
                    messageService.getMessage("error.salary.department.notfound", employeeId));
        }

        // 3. 各種手当の算出
        long gradeAllowance = calculateGradeAllowance(employee.getSkillGrade(), targetMonthEnd);
        long positionAllowance = calculatePositionAllowance(employee.getPositionId(), targetMonthEnd);
        long qualificationAllowance = calculateQualificationAllowance(employeeId, targetMonthEnd);
        long seniorityAllowance = calculateSeniorityAllowance(employee.getHireDate(), targetYearMonth);

        // 4. 給与総額の算出
        long totalSalary = gradeAllowance + positionAllowance + qualificationAllowance + seniorityAllowance;

        // 5. 登録
        SalaryResult result = new SalaryResult();
        result.setEmployeeId(employeeId);
        result.setTargetYear(targetYearMonth.getYear());
        result.setTargetMonth(targetYearMonth.getMonthValue());
        result.setDepartmentId(employee.getDepartmentId());
        result.setTotalWorkHours(workHours.getWorkHours());
        result.setTotalOvertimeHours(workHours.getOvertimeHours());
        result.setTotalSalary(totalSalary);
        salaryResultRepository.insert(result);
    }

    /** 職能資格給を算出する。対象年月時点で有効なskill_gradeマスタの手当額を取得する。 */
    public long calculateGradeAllowance(Integer skillGrade, LocalDate targetMonthEnd) {
        SkillGrade grade = skillGradeRepository.findEffectiveAt(skillGrade, targetMonthEnd);
        if (grade == null) {
            throw new SalaryCalculationException(messageService.getMessage("error.salary.skillgrade.notfound", skillGrade));
        }
        return grade.getAllowance();
    }

    /** 役職手当を算出する。役職が無い場合は0円。 */
    public long calculatePositionAllowance(Long positionId, LocalDate targetMonthEnd) {
        if (positionId == null) {
            return 0L;
        }
        Position position = positionRepository.findEffectiveAt(positionId, targetMonthEnd);
        if (position == null) {
            throw new SalaryCalculationException(messageService.getMessage("error.salary.position.notfound", positionId));
        }
        return position.getPositionAllowance();
    }

    /**
     * 資格手当を算出する。保有資格ごとに対象年月時点で有効な手当額を合算する。
     * 高度資格を2件以上保有する場合、取得日が2番目以降の高度資格は加算額を10,000円とする。
     */
    public long calculateQualificationAllowance(Long employeeId, LocalDate targetMonthEnd) {
        List<EmployeeQualification> heldQualifications =
                employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(employeeId);

        long total = 0L;
        int advanceCount = 0;

        for (EmployeeQualification held : heldQualifications) {
            Qualification qualification =
                    qualificationRepository.findEffectiveAt(held.getQualificationId(), targetMonthEnd);
            if (qualification == null) {
                throw new SalaryCalculationException(
                        messageService.getMessage("error.salary.qualification.notfound", held.getQualificationId()));
            }

            if (Boolean.TRUE.equals(qualification.getIsAdvance())) {
                advanceCount++;
                total += (advanceCount == 1)
                        ? qualification.getQualificationAllowance()
                        : SalaryConstants.ADVANCE_QUALIFICATION_ALLOWANCE;
            } else {
                total += qualification.getQualificationAllowance();
            }
        }
        return total;
    }

    /**
     * 所属年給を算出する（履歴テーブルを持たず、都度計算する）。
     * 初年度5,000円を起点とし、入社年度の翌年度から対象年度まで、4月1日（改定日）ごとに
     * 「その時点で入社から満1年経過しているか」を判定し、経過していれば5,000円を加算する。
     */
    public long calculateSeniorityAllowance(LocalDate hireDate, YearMonth targetYearMonth) {
        int hireFiscalYear = fiscalYearOf(YearMonth.from(hireDate));
        int targetFiscalYear = fiscalYearOf(targetYearMonth);

        long amount = SalaryConstants.SENIORITY_PAY_BASE;
        for (int fiscalYear = hireFiscalYear + 1; fiscalYear <= targetFiscalYear; fiscalYear++) {
            LocalDate revisionDate = LocalDate.of(fiscalYear, 4, 1);
            boolean oneYearCompleted = !hireDate.plusYears(1).isAfter(revisionDate);
            if (oneYearCompleted) {
                amount += SalaryConstants.SENIORITY_PAY_INCREMENT;
            }
        }
        return amount;
    }

    /** 4月始まりの年度を返す（1〜3月は前年扱い）。 */
    private int fiscalYearOf(YearMonth yearMonth) {
        return yearMonth.getMonthValue() >= 4 ? yearMonth.getYear() : yearMonth.getYear() - 1;
    }
}