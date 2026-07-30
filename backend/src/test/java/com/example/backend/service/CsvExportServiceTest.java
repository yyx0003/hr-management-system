package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.entity.Department;
import com.example.backend.entity.Employee;
import com.example.backend.entity.SalaryResult;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    private static final YearMonth TARGET_MONTH =
            YearMonth.of(2026, 7);
    private static final LocalDate TARGET_MONTH_END =
            LocalDate.of(2026, 7, 31);

    @Mock
    private SalaryResultRepository salaryResultRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private MessageService messageService;

    private CsvExportService service;

    @BeforeEach
    void setUp() {

        service =
                new CsvExportService(
                        salaryResultRepository,
                        employeeRepository,
                        departmentRepository,
                        messageService);
    }

    @Test
    void exportHrCsvCreatesSortedHistoricalCsvWithBom() {

        SalaryResult employeeTwoResult =
                createSalaryResult(
                        2L,
                        20L,
                        "1E+3",
                        "2.50",
                        320000L);
        SalaryResult employeeOneResult =
                createSalaryResult(
                        1L,
                        10L,
                        "160.00",
                        "10.25",
                        300000L);

        when(salaryResultRepository
                .findByTargetYearAndTargetMonthOrderByEmployeeId(
                        2026,
                        7))
                .thenReturn(
                        List.of(
                                employeeTwoResult,
                                employeeOneResult));

        Employee employeeTwo =
                createEmployee(
                        2L,
                        "E0002",
                        "退職 花子",
                        LocalDate.of(2026, 7, 15),
                        99L);
        Employee employeeOne =
                createEmployee(
                        1L,
                        "E0001",
                        "山田, \"太郎\"",
                        null,
                        99L);

        when(employeeRepository.findEffectiveAt(
                2L,
                TARGET_MONTH_END))
                .thenReturn(employeeTwo);
        when(employeeRepository.findEffectiveAt(
                1L,
                TARGET_MONTH_END))
                .thenReturn(employeeOne);

        Department snapshotDepartmentTen =
                createDepartment(
                        10L,
                        "営業,\"第一\"");
        Department snapshotDepartmentTwenty =
                createDepartment(
                        20L,
                        "管理\n本部");

        when(departmentRepository.findEffectiveAt(
                10L,
                TARGET_MONTH_END))
                .thenReturn(snapshotDepartmentTen);
        when(departmentRepository.findEffectiveAt(
                20L,
                TARGET_MONTH_END))
                .thenReturn(snapshotDepartmentTwenty);

        CsvFileData result =
                service.exportHrCsv(TARGET_MONTH);

        assertEquals(
                "人事向け_勤怠給与_202607.csv",
                result.getFileName());

        String csv =
                new String(
                        result.getContent(),
                        StandardCharsets.UTF_8);

        assertTrue(csv.startsWith(
                "\uFEFF社員番号,氏名,所属部署,"
                        + "稼働時間,残業時間,給与\r\n"));
        assertEquals(
                "\uFEFF社員番号,氏名,所属部署,稼働時間,残業時間,給与\r\n"
                        + "E0001,\"山田, \"\"太郎\"\"\","
                        + "\"営業,\"\"第一\"\"\",160.00,10.25,300000\r\n"
                        + "E0002,退職 花子,\"管理\n本部\","
                        + "1000,2.50,320000\r\n",
                csv);

        verify(employeeRepository).findEffectiveAt(
                2L,
                TARGET_MONTH_END);
        verify(departmentRepository).findEffectiveAt(
                20L,
                TARGET_MONTH_END);
    }

    @Test
    void exportHrCsvThrowsBusinessExceptionWhenNoSalaryResultExists() {

        when(salaryResultRepository
                .findByTargetYearAndTargetMonthOrderByEmployeeId(
                        2026,
                        7))
                .thenReturn(List.of());
        when(messageService.getMessage(
                "scr080.salaryResult.notfound"))
                .thenReturn(
                        "対象年月の給与実績データが存在しません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.exportHrCsv(
                                        TARGET_MONTH));

        assertEquals(
                "対象年月の給与実績データが存在しません。",
                exception.getMessage());
    }

    @Test
    void exportManagementCsvAggregatesBySnapshotDepartmentInIdOrder() {

        SalaryResult departmentTwentyFirst =
                createSalaryResult(
                        1L,
                        20L,
                        "1E+2",
                        "5.25",
                        200000L);
        SalaryResult departmentTenFirst =
                createSalaryResult(
                        2L,
                        10L,
                        "80.50",
                        "2.75",
                        150000L);
        SalaryResult departmentTwentySecond =
                createSalaryResult(
                        3L,
                        20L,
                        "90.25",
                        "3.00",
                        180000L);
        SalaryResult duplicateEmployee =
                createSalaryResult(
                        1L,
                        20L,
                        "10.00",
                        "1.00",
                        10000L);

        when(salaryResultRepository
                .findByTargetYearAndTargetMonthOrderByEmployeeId(
                        2026,
                        7))
                .thenReturn(
                        List.of(
                                departmentTwentyFirst,
                                departmentTenFirst,
                                departmentTwentySecond,
                                duplicateEmployee));

        when(departmentRepository.findEffectiveAt(
                10L,
                TARGET_MONTH_END))
                .thenReturn(
                        createDepartment(
                                10L,
                                "第一部"));
        when(departmentRepository.findEffectiveAt(
                20L,
                TARGET_MONTH_END))
                .thenReturn(
                        createDepartment(
                                20L,
                                "第二部"));

        CsvFileData result =
                service.exportManagementCsv(
                        TARGET_MONTH);

        assertEquals(
                "経営向け_部署別集計_202607.csv",
                result.getFileName());

        String csv =
                new String(
                        result.getContent(),
                        StandardCharsets.UTF_8);

        assertEquals(
                "\uFEFF部署名,所属人数,給与総額,"
                        + "稼働時間合計,残業時間合計\r\n"
                        + "第一部,1,150000,80.50,2.75\r\n"
                        + "第二部,2,390000,200.25,9.25\r\n",
                csv);
    }

    @Test
    void exportManagementCsvThrowsBusinessExceptionWhenNoSalaryResultExists() {

        when(salaryResultRepository
                .findByTargetYearAndTargetMonthOrderByEmployeeId(
                        2026,
                        7))
                .thenReturn(List.of());
        when(messageService.getMessage(
                "scr090.salaryResult.notfound"))
                .thenReturn(
                        "対象年月の給与実績データが存在しません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.exportManagementCsv(
                                        TARGET_MONTH));

        assertEquals(
                "対象年月の給与実績データが存在しません。",
                exception.getMessage());
    }

    private SalaryResult createSalaryResult(
            Long employeeId,
            Long departmentId,
            String totalWorkHours,
            String totalOvertimeHours,
            Long totalSalary) {

        SalaryResult result =
                new SalaryResult();
        result.setEmployeeId(employeeId);
        result.setTargetYear(2026);
        result.setTargetMonth(7);
        result.setDepartmentId(departmentId);
        result.setTotalWorkHours(
                new BigDecimal(totalWorkHours));
        result.setTotalOvertimeHours(
                new BigDecimal(
                        totalOvertimeHours));
        result.setTotalSalary(totalSalary);
        return result;
    }

    private Employee createEmployee(
            Long employeeId,
            String employeeNo,
            String employeeName,
            LocalDate retireDate,
            Long currentDepartmentId) {

        Employee employee =
                new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeNo(employeeNo);
        employee.setEmployeeName(employeeName);
        employee.setRetireDate(retireDate);
        employee.setDepartmentId(
                currentDepartmentId);
        return employee;
    }

    private Department createDepartment(
            Long departmentId,
            String departmentName) {

        Department department =
                new Department();
        department.setDepartmentId(departmentId);
        department.setDepartmentName(
                departmentName);
        return department;
    }
}
