package com.example.backend.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCreateRequest;
import com.example.backend.dto.attendance.AttendanceCsvImportResponse;
import com.example.backend.dto.attendance.AttendanceListResponse;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.service.AttendanceCsvExportService;
import com.example.backend.service.AttendanceCsvImportService;
import com.example.backend.service.AttendanceRegistrationService;
import com.example.backend.service.AttendanceUpdateService;
import com.example.backend.service.EmployeeService;
import com.example.backend.service.MonthlyAttendanceListService;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠入力画面用のAPI。
 */
@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final MonthlyAttendanceListService monthlyAttendanceListService;
    private final AttendanceRegistrationService attendanceRegistrationService;
    private final AttendanceUpdateService attendanceUpdateService;
    private final AttendanceCsvImportService attendanceCsvImportService;
    private final AttendanceCsvExportService attendanceCsvExportService;
    private final EmployeeService employeeService;
    private final MessageService messageService;

    /**
     * ログイン社員の月次勤怠一覧を取得する。
     *
     * GET /api/attendances?targetMonth=2026-07
     *
     * @param targetMonth 対象年月（yyyy-MM）
     * @param principal ログイン情報
     * @return 月次勤怠一覧
     */
    @GetMapping
    public ResponseEntity<AttendanceListResponse> getMonthlyAttendanceList(
            @RequestParam(required = false) String targetMonth,
            Principal principal) {

        Long employeeId =
                getLoginEmployeeId(principal);

        YearMonth targetYearMonth =
                parseTargetMonth(targetMonth);

        AttendanceListResponse response =
                monthlyAttendanceListService
                        .getMonthlyAttendanceList(
                                employeeId,
                                targetYearMonth);

        return ResponseEntity.ok(response);
    }

    /**
     * ログイン社員の勤怠情報を登録する。
     *
     * POST /api/attendances
     *
     * @param request 登録内容
     * @param principal ログイン情報
     * @return 201 Created
     */
    @PostMapping
    public ResponseEntity<Void> registerAttendance(
            @RequestBody AttendanceCreateRequest request,
            Principal principal) {

        Long employeeId =
                getLoginEmployeeId(principal);

        attendanceRegistrationService.register(
                employeeId,
                request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .build();
    }

    /**
     * ログイン社員の勤怠情報を更新する。
     *
     * PUT /api/attendances/{workDate}
     *
     * @param workDate 勤務日（yyyy-MM-dd）
     * @param request 更新内容
     * @param principal ログイン情報
     * @return 204 No Content
     */
    @PutMapping("/{workDate}")
    public ResponseEntity<Void> updateAttendance(
            @PathVariable String workDate,
            @RequestBody AttendanceUpdateRequest request,
            Principal principal) {

        Long employeeId =
                getLoginEmployeeId(principal);

        LocalDate parsedWorkDate =
                parseWorkDate(workDate);

        attendanceUpdateService.update(
                employeeId,
                parsedWorkDate,
                request);

        return ResponseEntity.noContent().build();
    }

    /**
     * ログイン社員の勤怠CSVを出力する。
     *
     * GET /api/attendances/csv-export?targetMonth=2026-07
     *
     * @param targetMonth 出力対象年月（yyyy-MM）
     * @param principal ログイン情報
     * @return 勤怠CSVファイル
     */
    @GetMapping(
            value = "/csv-export",
            produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportAttendanceCsv(
            @RequestParam(required = false)
                    String targetMonth,
            Principal principal) {

        Long employeeId =
                getLoginEmployeeId(principal);

        String employeeNo =
                principal.getName();

        YearMonth targetYearMonth =
                parseCsvExportTargetMonth(targetMonth);

        CsvFileData csvFile =
                attendanceCsvExportService.exportCsv(
                        employeeId,
                        employeeNo,
                        targetYearMonth);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                csvFile.getFileName(),
                                StandardCharsets.UTF_8)
                        .build());
        headers.setContentType(
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8));
        headers.setContentLength(
                csvFile.getContent().length);

        return new ResponseEntity<>(
                csvFile.getContent(),
                headers,
                HttpStatus.OK);
    }

    /**
     * ログイン社員の勤怠CSVを取り込む。
     *
     * POST /api/attendances/csv-import
     *
     * @param targetMonth 取込対象年月（yyyy-MM）
     * @param file CSVファイル
     * @param principal ログイン情報
     * @return CSV取込結果
     */
    @PostMapping(
            value = "/csv-import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttendanceCsvImportResponse>
            importAttendanceCsv(
                    @RequestParam(required = false)
                            String targetMonth,
                    @RequestParam(required = false)
                            MultipartFile file,
                    Principal principal) {

        Long employeeId =
                getLoginEmployeeId(principal);

        String employeeNo =
                principal.getName();

        YearMonth targetYearMonth =
                parseCsvTargetMonth(targetMonth);

        AttendanceCsvImportResponse response =
                attendanceCsvImportService.importCsv(
                        employeeId,
                        employeeNo,
                        targetYearMonth,
                        file);

        return ResponseEntity.ok(response);
    }

    /**
     * ログイン社員IDを取得する。
     *
     * JWTのsubjectには社員番号が設定されているため、
     * 社員詳細取得処理から社員IDを取得する。
     *
     * @param principal ログイン情報
     * @return 社員ID
     */
    private Long getLoginEmployeeId(
            Principal principal) {

        if (principal == null
                || principal.getName() == null
                || principal.getName().isBlank()) {

            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    messageService.getMessage(
                            "error.authentication.required"));
        }

        EmployeeDetailDTO employee =
                employeeService.getEmployeeDetail(
                        principal.getName());

        return employee.employeeId();
    }

    /**
     * 対象年月をYearMonthへ変換する。
     *
     * @param targetMonth 対象年月文字列
     * @return 対象年月
     */
    private YearMonth parseTargetMonth(
            String targetMonth) {

        if (targetMonth == null
                || targetMonth.isBlank()) {

            throw new BusinessException(
                    messageService.getMessage(
                            "scr060.targetMonth.required"));
        }

        try {
            return YearMonth.parse(targetMonth);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.targetMonth.invalid"));
        }
    }

    /**
     * CSV出力対象年月をYearMonthへ変換する。
     *
     * @param targetMonth 対象年月文字列
     * @return 対象年月
     */
    private YearMonth parseCsvExportTargetMonth(
            String targetMonth) {

        if (targetMonth == null
                || targetMonth.isBlank()) {

            throw new BusinessException(
                    messageService.getMessage(
                            "scr070.export.targetMonth.required"));
        }

        try {
            return YearMonth.parse(targetMonth);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.targetMonth.invalid"));
        }
    }

    /**
     * CSV取込対象年月をYearMonthへ変換する。
     *
     * @param targetMonth 対象年月文字列
     * @return 対象年月
     */
    private YearMonth parseCsvTargetMonth(
            String targetMonth) {

        if (targetMonth == null
                || targetMonth.isBlank()) {

            throw new BusinessException(
                    messageService.getMessage(
                            "scr070.targetMonth.required"));
        }

        try {
            return YearMonth.parse(targetMonth);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.targetMonth.invalid"));
        }
    }

    /**
     * 勤務日をLocalDateへ変換する。
     *
     * @param workDate 勤務日文字列
     * @return 勤務日
     */
    private LocalDate parseWorkDate(
            String workDate) {

        try {
            return LocalDate.parse(workDate);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workDate.invalid"));
        }
    }
}