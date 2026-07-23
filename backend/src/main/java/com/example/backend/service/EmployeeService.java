package com.example.backend.service;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.dto.employee.QualificationDetailDTO;
import com.example.backend.entity.Employee;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;

// ---- 胡追加分ここから ----
import java.time.YearMonth;
import java.util.List;
// ---- 胡追加分ここまで ----

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeQualificationRepository employeeQualificationRepository;
    private final MessageService messageService;

    public java.util.List<EmployeeListDTO> searchEmployees(
            String employeeNo, String employeeName, Long departmentId) {
        return employeeRepository.searchEffectiveAndEmployed(
                escapeLike(employeeNo),
                escapeLike(employeeName),
                departmentId,
                LocalDate.now());
    }

    public EmployeeDetailDTO getEmployeeDetail(String employeeNo) {
        Employee employee = getEffectiveEmployee(employeeNo, LocalDate.now());

        var qualifications = employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(employee.getEmployeeId())
                .stream()
                .map(qualification -> new QualificationDetailDTO(
                        qualification.getQualificationId(),
                        qualification.getAcquisitionDate()))
                .toList();

        return new EmployeeDetailDTO(
                employee.getEmployeeId(),
                employee.getEmployeeNo(),
                employee.getEmployeeName(),
                employee.getDepartmentId(),
                employee.getBirthDate(),
                employee.getPostalCode(),
                employee.getAddress(),
                employee.getPhoneNumber(),
                employee.getEmailAddress(),
                employee.getHireDate(),
                employee.getRetireDate(),
                employee.getPositionId(),
                employee.getSkillGrade(),
                qualifications);
    }

    public Employee getEffectiveEmployee(String employeeNo, LocalDate referenceDate) {
        Employee employee = employeeRepository.findEffectiveAndEmployedByEmployeeNoAt(
                employeeNo,
                referenceDate);

        if (employee == null) {
            String message = messageService.getMessage("error.employee.notfound", employeeNo);
            throw new BusinessException(HttpStatus.NOT_FOUND, message);
        }

        return employee;
    }
    // ==========================================================
    // 胡追加分ここから（給与計算・退職者削除バッチ用）
    // ==========================================================

    /**
     * 対象年月に在籍していた社員のうち、salary_resultが未登録の社員IDを取得する（給与計算対象）。
     * 再実行時は既に登録済みの社員が自動的に除外されるため、そのまま「補完実行」としても使える。
     */
    public List<Long> findEligibleEmployeeIds(YearMonth targetYearMonth) {
        LocalDate targetMonthStart = targetYearMonth.atDay(1);
        LocalDate targetMonthEnd = targetYearMonth.atEndOfMonth();
        return employeeRepository.findEligibleEmployeeIds(
                targetMonthStart, targetMonthEnd, targetYearMonth.getYear(), targetYearMonth.getMonthValue());
    }

    /** 削除基準日以前に退職した社員のIDを、重複を除いて取得する（退職者削除バッチ対象）。 */
    public List<Long> findRetireeIds(LocalDate cutoffDate) {
        return employeeRepository.findRetireeIds(cutoffDate);
    }

    /**
     * 対象社員の対象年月末日時点で有効なemployeeレコードを取得する。
     * この1行にdepartmentId・skillGrade・positionIdがまとめて含まれる。
     */
    public Employee findEffectiveEmployeeAt(Long employeeId, LocalDate targetMonthEnd) {
        return employeeRepository.findEffectiveAt(employeeId, targetMonthEnd);
    }

    /** 対象employeeIdの全履歴行を削除する（退職者削除バッチで使用）。 */
    public void deleteByEmployeeId(Long employeeId) {
        employeeRepository.deleteByEmployeeId(employeeId);
    }

    // ==========================================================
    // 胡追加分ここまで
    // ==========================================================

    private String escapeLike(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
