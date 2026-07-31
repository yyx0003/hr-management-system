package com.example.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.backend.common.MessageService;
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

/**
 * SalaryCalculationServiceの単体テスト。
 * DB・他Serviceへの依存は全てMockitoでモック化する（Springコンテキストは起動しない）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalaryCalculationServiceTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private WorkHoursCalculationService workHoursCalculationService;
    @Mock
    private SkillGradeRepository skillGradeRepository;
    @Mock
    private PositionRepository positionRepository;
    @Mock
    private QualificationRepository qualificationRepository;
    @Mock
    private EmployeeQualificationRepository employeeQualificationRepository;
    @Mock
    private SalaryResultRepository salaryResultRepository;
    @Mock
    private MessageService messageService;

    private SalaryCalculationService target;

    @BeforeEach
    void setUp() {
        target = new SalaryCalculationService(
                employeeService,
                workHoursCalculationService,
                skillGradeRepository,
                positionRepository,
                qualificationRepository,
                employeeQualificationRepository,
                salaryResultRepository,
                messageService);
        // 例外メッセージの中身はテスト対象外のため、キーをそのまま返すダミー実装にする
        lenient().when(messageService.getMessage(any(), any())).thenReturn("dummy message");
    }

    // ------------------------------------------------------------------
    // calculateGradeAllowance
    // ------------------------------------------------------------------

    @Test
    void calculateGradeAllowance_見つかった場合は手当額を返す() {
        SkillGrade grade = new SkillGrade();
        grade.setSkillGrade(3);
        grade.setAllowance(250_000L);
        when(skillGradeRepository.findEffectiveAt(3, LocalDate.of(2026, 7, 31))).thenReturn(grade);

        long result = target.calculateGradeAllowance(3, LocalDate.of(2026, 7, 31));

        assertThat(result).isEqualTo(250_000L);
    }

    @Test
    void calculateGradeAllowance_見つからない場合は例外を送出する() {
        when(skillGradeRepository.findEffectiveAt(99, LocalDate.of(2026, 7, 31))).thenReturn(null);

        assertThatThrownBy(() -> target.calculateGradeAllowance(99, LocalDate.of(2026, 7, 31)))
                .isInstanceOf(SalaryCalculationException.class);
    }

    // ------------------------------------------------------------------
    // calculatePositionAllowance
    // ------------------------------------------------------------------

    @Test
    void calculatePositionAllowance_役職IDがnullの場合は0円() {
        long result = target.calculatePositionAllowance(null, LocalDate.of(2026, 7, 31));

        assertThat(result).isZero();
    }

    @Test
    void calculatePositionAllowance_見つかった場合は手当額を返す() {
        Position position = new Position();
        position.setPositionId(1L);
        position.setPositionAllowance(50_000L);
        when(positionRepository.findEffectiveAt(1L, LocalDate.of(2026, 7, 31))).thenReturn(position);

        long result = target.calculatePositionAllowance(1L, LocalDate.of(2026, 7, 31));

        assertThat(result).isEqualTo(50_000L);
    }

    @Test
    void calculatePositionAllowance_IDはあるが見つからない場合は例外を送出する() {
        when(positionRepository.findEffectiveAt(99L, LocalDate.of(2026, 7, 31))).thenReturn(null);

        assertThatThrownBy(() -> target.calculatePositionAllowance(99L, LocalDate.of(2026, 7, 31)))
                .isInstanceOf(SalaryCalculationException.class);
    }

    // ------------------------------------------------------------------
    // calculateQualificationAllowance
    // ------------------------------------------------------------------

    @Test
    void calculateQualificationAllowance_保有資格が無い場合は0円() {
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of());

        long result = target.calculateQualificationAllowance(1L, LocalDate.of(2026, 7, 31));

        assertThat(result).isZero();
    }

    @Test
    void calculateQualificationAllowance_通常資格は手当額をそのまま合算する() {
        EmployeeQualification held = heldQualification(1L, 10L, LocalDate.of(2020, 1, 1));
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of(held));
        when(qualificationRepository.findEffectiveAt(10L, LocalDate.of(2026, 7, 31)))
                .thenReturn(qualification(10L, "基本情報技術者試験", false, 3_000L));

        long result = target.calculateQualificationAllowance(1L, LocalDate.of(2026, 7, 31));

        assertThat(result).isEqualTo(3_000L);
    }

    @Test
    void calculateQualificationAllowance_高度資格を2件保有する場合は2件目以降を1万円で計算する() {
        EmployeeQualification held1 = heldQualification(1L, 20L, LocalDate.of(2020, 1, 1));
        EmployeeQualification held2 = heldQualification(1L, 21L, LocalDate.of(2021, 1, 1));
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of(held1, held2));
        when(qualificationRepository.findEffectiveAt(20L, LocalDate.of(2026, 7, 31)))
                .thenReturn(qualification(20L, "システムアーキテクト", true, 30_000L));
        when(qualificationRepository.findEffectiveAt(21L, LocalDate.of(2026, 7, 31)))
                .thenReturn(qualification(21L, "プロジェクトマネージャ", true, 30_000L));

        long result = target.calculateQualificationAllowance(1L, LocalDate.of(2026, 7, 31));

        // 1件目は本来の手当額（30,000円）、2件目は固定10,000円 → 合計40,000円
        assertThat(result).isEqualTo(40_000L);
    }

    @Test
    void calculateQualificationAllowance_資格マスタが見つからない場合は例外を送出する() {
        EmployeeQualification held = heldQualification(1L, 99L, LocalDate.of(2020, 1, 1));
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of(held));
        when(qualificationRepository.findEffectiveAt(99L, LocalDate.of(2026, 7, 31))).thenReturn(null);

        assertThatThrownBy(() -> target.calculateQualificationAllowance(1L, LocalDate.of(2026, 7, 31)))
                .isInstanceOf(SalaryCalculationException.class);
    }

    // ------------------------------------------------------------------
    // calculateSeniorityAllowance
    // ------------------------------------------------------------------

    @Test
    void calculateSeniorityAllowance_入社1年未満は初年度額のまま() {
        // 2024/10/01入社、2025/04時点ではまだ1年経過していない
        long result = target.calculateSeniorityAllowance(LocalDate.of(2024, 10, 1), YearMonth.of(2025, 4));

        assertThat(result).isEqualTo(5_000L);
    }

    @Test
    void calculateSeniorityAllowance_満1年経過後の4月改定で加算される() {
        // 2024/10/01入社、2026/04時点で満1年経過済み → 1回加算
        long result = target.calculateSeniorityAllowance(LocalDate.of(2024, 10, 1), YearMonth.of(2026, 4));

        assertThat(result).isEqualTo(10_000L);
    }

    @Test
    void calculateSeniorityAllowance_複数回の4月改定を経ると加算が積み上がる() {
        // 2024/10/01入社、2027/04時点 → 2026/04・2027/04の2回加算
        long result = target.calculateSeniorityAllowance(LocalDate.of(2024, 10, 1), YearMonth.of(2027, 4));

        assertThat(result).isEqualTo(15_000L);
    }

    @Test
    void calculateSeniorityAllowance_4月1日入社は当年度4月から満1年経過扱いにならない() {
        // 4/1入社の場合、翌年度の4/1時点でちょうど満1年 → 加算される
        long result = target.calculateSeniorityAllowance(LocalDate.of(2025, 4, 1), YearMonth.of(2026, 4));

        assertThat(result).isEqualTo(10_000L);
    }

    // ------------------------------------------------------------------
    // calculateAll：スキップ動作の確認
    // ------------------------------------------------------------------

    @Test
    void calculateAll_一部社員の計算に失敗しても他の社員の処理は継続する() {
        when(employeeService.findEligibleEmployeeIds(YearMonth.of(2026, 7)))
                .thenReturn(List.of(1L, 2L, 3L));

        // 1番目と3番目は正常なemployee、2番目はfindEffectiveEmployeeAtがnullを返す（データ不整合）
        Employee normalEmployee = employee(1L, 1L, 3, null);
        Employee normalEmployee3 = employee(3L, 1L, 3, null);
        when(employeeService.findEffectiveEmployeeAt(1L, LocalDate.of(2026, 7, 31))).thenReturn(normalEmployee);
        when(employeeService.findEffectiveEmployeeAt(2L, LocalDate.of(2026, 7, 31))).thenReturn(null);
        when(employeeService.findEffectiveEmployeeAt(3L, LocalDate.of(2026, 7, 31))).thenReturn(normalEmployee3);

        SkillGrade grade = new SkillGrade();
        grade.setSkillGrade(3);
        grade.setAllowance(250_000L);
        when(skillGradeRepository.findEffectiveAt(3, LocalDate.of(2026, 7, 31))).thenReturn(grade);
        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(any()))
                .thenReturn(List.of());
        when(workHoursCalculationService.calculateWorkHours(any(), any(), any()))
                .thenReturn(com.example.backend.dto.attendance.WorkHoursResult.zero());

        var result = target.calculateAll(2026, 7);

        assertThat(result.processedCount()).isEqualTo(2);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skippedEmployees()).hasSize(1);
        assertThat(result.skippedEmployees().get(0).employeeId()).isEqualTo(2L);

        // 正常な2名分はinsertが呼ばれ、スキップされた1名分は呼ばれない
        verify(salaryResultRepository, times(2)).insert(any(SalaryResult.class));
    }

    // ------------------------------------------------------------------
    // calculateOne：時給ベースの給与計算
    // ------------------------------------------------------------------

    @Test
    void calculateOne_時給は職能資格給と所属年給の合計を160Hで割って切上げる() {
        // gradeAllowance=155,000円 + seniorityAllowance=5,000円 = 160,000円 → 時給=1,000円（割り切れるケース）
        setUpBasicEmployee(155_000L);
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(160), java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 残業・休日出勤・不足いずれも0時間のため、総額は固定手当合計のみ = 155,000 + 5,000 = 160,000
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(160_000L);
    }

    @Test
    void calculateOne_時給計算で小数点以下は切上げになる() {
        // gradeAllowance=95,001円 + seniorityAllowance=5,000円 = 100,001円
        setUpBasicEmployee(95_001L);
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(160), java.math.BigDecimal.ONE,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 100,001÷160=625.00625... → 切上げで626円。残業1時間×1.25倍=782.5円→四捨五入で783円
        // 総額 = 100,001（固定手当） + 783（残業代） = 100,784
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(100_784L);
    }

    @Test
    void calculateOne_残業代は時給の1_25倍で計算される() {
        setUpBasicEmployee(155_000L); // + seniority 5,000円 = 160,000円 → 時給1,000円
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(160), java.math.BigDecimal.valueOf(10),
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 残業代 = 1,000円 × 10時間 × 1.25 = 12,500円 → 総額 = 160,000 + 12,500 = 172,500
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(172_500L);
    }

    @Test
    void calculateOne_休日出勤代は時給の1_5倍で計算される() {
        setUpBasicEmployee(155_000L); // 時給1,000円
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(160), java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.valueOf(8), java.math.BigDecimal.ZERO));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 休日出勤代 = 1,000円 × 8時間 × 1.5 = 12,000円 → 総額 = 160,000 + 12,000 = 172,000
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(172_000L);
        assertThat(captor.getValue().getTotalHolidayWorkHours())
                .isEqualByComparingTo(java.math.BigDecimal.valueOf(8));
    }

    @Test
    void calculateOne_欠勤等の不足時間は時給換算で給与から控除される() {
        setUpBasicEmployee(155_000L); // 時給1,000円
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(152), java.math.BigDecimal.ZERO,
                        java.math.BigDecimal.ZERO, java.math.BigDecimal.valueOf(8)));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 欠勤控除額 = 1,000円 × 8時間 = 8,000円 → 総額 = 160,000 − 8,000 = 152,000
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(152_000L);
    }

    @Test
    void calculateOne_残業_休日出勤_欠勤控除が同時に発生した場合まとめて計算される() {
        setUpBasicEmployee(155_000L); // 時給1,000円
        when(workHoursCalculationService.calculateWorkHours(1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .thenReturn(new com.example.backend.dto.attendance.WorkHoursResult(
                        java.math.BigDecimal.valueOf(150), java.math.BigDecimal.valueOf(5),
                        java.math.BigDecimal.valueOf(3), java.math.BigDecimal.valueOf(2)));

        target.calculateOne(1L, YearMonth.of(2026, 7));

        ArgumentCaptor<SalaryResult> captor = ArgumentCaptor.forClass(SalaryResult.class);
        verify(salaryResultRepository).insert(captor.capture());
        // 残業代=1,000×5×1.25=6,250円、休日出勤代=1,000×3×1.5=4,500円、欠勤控除=1,000×2=2,000円
        // 総額 = 160,000 + 6,250 + 4,500 − 2,000 = 168,750
        assertThat(captor.getValue().getTotalSalary()).isEqualTo(168_750L);
    }

    /**
     * 時給計算テスト用に、gradeAllowance以外を全て0円に固定したemployee/skillGrade/qualificationのモックを準備する。
     * hireDateは十分過去にし、seniorityAllowanceが常に基本額5,000円のみになるようにする
     * （4月改定を一度も跨がない期間に設定）。
     */
    private void setUpBasicEmployee(long gradeAllowance) {
        Employee employee = employee(1L, 1L, 3, null);
        employee.setHireDate(LocalDate.of(2026, 5, 1)); // 2026/7時点でまだ4月改定を跨いでいない → seniorityAllowance=5,000円固定

        when(employeeService.findEffectiveEmployeeAt(1L, LocalDate.of(2026, 7, 31))).thenReturn(employee);

        SkillGrade grade = new SkillGrade();
        grade.setSkillGrade(3);
        grade.setAllowance(gradeAllowance);
        when(skillGradeRepository.findEffectiveAt(3, LocalDate.of(2026, 7, 31))).thenReturn(grade);

        when(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(1L))
                .thenReturn(List.of());
    }

    // ------------------------------------------------------------------
    // テストデータ作成用ヘルパー
    // ------------------------------------------------------------------

    private EmployeeQualification heldQualification(Long employeeId, Long qualificationId, LocalDate acquisitionDate) {
        EmployeeQualification held = new EmployeeQualification();
        held.setEmployeeId(employeeId);
        held.setQualificationId(qualificationId);
        held.setAcquisitionDate(acquisitionDate);
        return held;
    }

    private Qualification qualification(Long id, String name, boolean isAdvance, long allowance) {
        Qualification q = new Qualification();
        q.setQualificationId(id);
        q.setQualificationName(name);
        q.setIsAdvance(isAdvance);
        q.setQualificationAllowance(allowance);
        return q;
    }

    private Employee employee(Long employeeId, Long departmentId, Integer skillGrade, Long positionId) {
        Employee e = new Employee();
        e.setEmployeeId(employeeId);
        e.setDepartmentId(departmentId);
        e.setSkillGrade(skillGrade);
        e.setPositionId(positionId);
        e.setHireDate(LocalDate.of(2020, 4, 1));
        return e;
    }
}