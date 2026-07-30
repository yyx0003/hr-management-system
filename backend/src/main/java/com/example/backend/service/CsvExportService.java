package com.example.backend.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.entity.Department;
import com.example.backend.entity.Employee;
import com.example.backend.entity.SalaryResult;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.repository.SalaryResultRepository;

import lombok.RequiredArgsConstructor;

/**
 * 人事向け・経営向けCSVを作成するサービス。
 */
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private static final String UTF_8_BOM = "\uFEFF";
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String HR_HEADER =
            "社員番号,氏名,所属部署,稼働時間,残業時間,給与";
    private static final String MANAGEMENT_HEADER =
            "部署名,所属人数,給与総額,稼働時間合計,残業時間合計";

    private final SalaryResultRepository salaryResultRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final MessageService messageService;

    /**
     * 人事向けCSVを作成する。
     *
     * @param targetMonth 対象年月
     * @return CSVファイル情報
     */
    public CsvFileData exportHrCsv(YearMonth targetMonth) {

        List<SalaryResult> salaryResults =
                findSalaryResults(
                        targetMonth,
                        "scr080.salaryResult.notfound");
        LocalDate targetMonthEnd =
                targetMonth.atEndOfMonth();
        Map<Long, Department> departments =
                new LinkedHashMap<>();
        List<HrCsvRow> rows =
                new ArrayList<>();

        for (SalaryResult salaryResult : salaryResults) {
            Employee employee =
                    employeeRepository.findEffectiveAt(
                            salaryResult.getEmployeeId(),
                            targetMonthEnd);

            if (employee == null) {
                throw new BusinessException(
                        messageService.getMessage(
                                "error.csv.employeeHistory.notfound",
                                salaryResult.getEmployeeId()));
            }

            Department department =
                    departments.computeIfAbsent(
                            salaryResult.getDepartmentId(),
                            departmentId ->
                                    findDepartment(
                                            departmentId,
                                            targetMonthEnd));

            rows.add(
                    new HrCsvRow(
                            employee.getEmployeeNo(),
                            employee.getEmployeeName(),
                            department.getDepartmentName(),
                            salaryResult.getTotalWorkHours(),
                            salaryResult.getTotalOvertimeHours(),
                            salaryResult.getTotalSalary()));
        }

        rows.sort(
                Comparator.comparing(
                        HrCsvRow::employeeNo));

        StringBuilder csv =
                createCsv(HR_HEADER);

        for (HrCsvRow row : rows) {
            appendRow(
                    csv,
                    row.employeeNo(),
                    row.employeeName(),
                    row.departmentName(),
                    toPlainString(row.totalWorkHours()),
                    toPlainString(row.totalOvertimeHours()),
                    String.valueOf(row.totalSalary()));
        }

        return createCsvFile(
                String.format(
                        "人事向け_勤怠給与_%s.csv",
                        formatFileMonth(targetMonth)),
                csv);
    }

    /**
     * 経営向けCSVを作成する。
     *
     * @param targetMonth 対象年月
     * @return CSVファイル情報
     */
    public CsvFileData exportManagementCsv(
            YearMonth targetMonth) {

        List<SalaryResult> salaryResults =
                findSalaryResults(
                        targetMonth,
                        "scr090.salaryResult.notfound");
        Map<Long, ManagementSummary> summaries =
                new java.util.TreeMap<>();

        for (SalaryResult salaryResult : salaryResults) {
            summaries.computeIfAbsent(
                            salaryResult.getDepartmentId(),
                            ignored ->
                                    new ManagementSummary())
                    .add(salaryResult);
        }

        LocalDate targetMonthEnd =
                targetMonth.atEndOfMonth();
        StringBuilder csv =
                createCsv(MANAGEMENT_HEADER);

        for (Map.Entry<Long, ManagementSummary> entry
                : summaries.entrySet()) {

            Department department =
                    findDepartment(
                            entry.getKey(),
                            targetMonthEnd);
            ManagementSummary summary =
                    entry.getValue();

            appendRow(
                    csv,
                    department.getDepartmentName(),
                    String.valueOf(
                            summary.employeeIds.size()),
                    String.valueOf(summary.totalSalary),
                    toPlainString(summary.totalWorkHours),
                    toPlainString(
                            summary.totalOvertimeHours));
        }

        return createCsvFile(
                String.format(
                        "経営向け_部署別集計_%s.csv",
                        formatFileMonth(targetMonth)),
                csv);
    }

    private List<SalaryResult> findSalaryResults(
            YearMonth targetMonth,
            String notFoundMessageKey) {

        List<SalaryResult> salaryResults =
                salaryResultRepository
                        .findByTargetYearAndTargetMonthOrderByEmployeeId(
                                targetMonth.getYear(),
                                targetMonth.getMonthValue());

        if (salaryResults.isEmpty()) {
            throw new BusinessException(
                    messageService.getMessage(
                            notFoundMessageKey));
        }

        return salaryResults;
    }

    private Department findDepartment(
            Long departmentId,
            LocalDate targetMonthEnd) {

        Department department =
                departmentRepository.findEffectiveAt(
                        departmentId,
                        targetMonthEnd);

        if (department == null) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.csv.departmentHistory.notfound",
                            departmentId));
        }

        return department;
    }

    private StringBuilder createCsv(String header) {

        return new StringBuilder()
                .append(UTF_8_BOM)
                .append(header)
                .append(LINE_SEPARATOR);
    }

    private void appendRow(
            StringBuilder csv,
            String... values) {

        for (int index = 0;
                index < values.length;
                index++) {

            if (index > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values[index]));
        }
        csv.append(LINE_SEPARATOR);
    }

    private String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        if (!value.contains(",")
                && !value.contains("\"")
                && !value.contains("\r")
                && !value.contains("\n")) {

            return value;
        }

        return "\""
                + value.replace("\"", "\"\"")
                + "\"";
    }

    private String toPlainString(
            BigDecimal value) {

        return value.toPlainString();
    }

    private String formatFileMonth(
            YearMonth targetMonth) {

        return String.format(
                "%04d%02d",
                targetMonth.getYear(),
                targetMonth.getMonthValue());
    }

    private CsvFileData createCsvFile(
            String fileName,
            StringBuilder csv) {

        return new CsvFileData(
                fileName,
                csv.toString()
                        .getBytes(StandardCharsets.UTF_8));
    }

    private record HrCsvRow(
            String employeeNo,
            String employeeName,
            String departmentName,
            BigDecimal totalWorkHours,
            BigDecimal totalOvertimeHours,
            Long totalSalary) {
    }

    private static class ManagementSummary {

        private final java.util.Set<Long> employeeIds =
                new java.util.HashSet<>();
        private long totalSalary;
        private BigDecimal totalWorkHours =
                BigDecimal.ZERO;
        private BigDecimal totalOvertimeHours =
                BigDecimal.ZERO;

        private void add(SalaryResult salaryResult) {

            employeeIds.add(
                    salaryResult.getEmployeeId());
            totalSalary +=
                    salaryResult.getTotalSalary();
            totalWorkHours =
                    totalWorkHours.add(
                            salaryResult.getTotalWorkHours());
            totalOvertimeHours =
                    totalOvertimeHours.add(
                            salaryResult
                                    .getTotalOvertimeHours());
        }
    }
}
