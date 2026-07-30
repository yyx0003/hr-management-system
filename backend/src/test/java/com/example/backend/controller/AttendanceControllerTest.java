package com.example.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.GlobalExceptionHandler;
import com.example.backend.dto.attendance.AttendanceCreateRequest;
import com.example.backend.dto.attendance.AttendanceCsvImportResponse;
import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.dto.attendance.AttendanceListResponse;
import com.example.backend.dto.attendance.AttendanceMonthlyDeleteResponse;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.dto.attendance.CsvImportError;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.service.AttendanceCsvExportService;
import com.example.backend.service.AttendanceCsvImportService;
import com.example.backend.service.AttendanceMonthlyDeleteService;
import com.example.backend.service.AttendanceRegistrationService;
import com.example.backend.service.AttendanceUpdateService;
import com.example.backend.service.EmployeeService;
import com.example.backend.service.MonthlyAttendanceListService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class AttendanceControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private MonthlyAttendanceListService monthlyAttendanceListService;

    @Mock
    private AttendanceRegistrationService attendanceRegistrationService;

    @Mock
    private AttendanceUpdateService attendanceUpdateService;

    @Mock
    private AttendanceCsvImportService attendanceCsvImportService;

    @Mock
    private AttendanceCsvExportService attendanceCsvExportService;

    @Mock
    private AttendanceMonthlyDeleteService attendanceMonthlyDeleteService;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private MessageService messageService;

    @Mock
    private Principal principal;

    @BeforeEach
    void setUp() {

        objectMapper =
                new ObjectMapper()
                        .registerModule(new JavaTimeModule())
                        .disable(
                                SerializationFeature
                                        .WRITE_DATES_AS_TIMESTAMPS);

        AttendanceController controller =
                new AttendanceController(
                monthlyAttendanceListService,
                attendanceRegistrationService,
                attendanceUpdateService,
                attendanceCsvImportService,
                attendanceCsvExportService,
                attendanceMonthlyDeleteService,
                employeeService,
                messageService);

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .setControllerAdvice(
                                new GlobalExceptionHandler(
                                        messageService))
                        .setMessageConverters(
                                new MappingJackson2HttpMessageConverter(
                                        objectMapper),
                                new ByteArrayHttpMessageConverter())
                        .build();
    }

    @Test
    void getMonthlyAttendanceListReturnsResponse()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        AttendanceListItem item =
                new AttendanceListItem(
                        "2026-07-01",
                        "09:00",
                        "18:00",
                        "NORMAL",
                        null,
                        null);

        AttendanceListResponse response =
                new AttendanceListResponse(
                        "2026-07",
                        List.of(item));

        when(monthlyAttendanceListService
                .getMonthlyAttendanceList(
                        1L,
                        YearMonth.of(2026, 7)))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026-07")
                                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.targetMonth")
                                .value("2026-07"))
                .andExpect(
                        jsonPath(
                                "$.attendanceList[0].workDate")
                                .value("2026-07-01"))
                .andExpect(
                        jsonPath(
                                "$.attendanceList[0].attendanceTime")
                                .value("09:00"))
                .andExpect(
                        jsonPath(
                                "$.attendanceList[0].leavingTime")
                                .value("18:00"))
                .andExpect(
                        jsonPath(
                                "$.attendanceList[0].workType")
                                .value("NORMAL"));

        verify(employeeService)
                .getEmployeeDetail("1924");

        verify(monthlyAttendanceListService)
                .getMonthlyAttendanceList(
                        1L,
                        YearMonth.of(2026, 7));
    }

    @Test
    void getMonthlyAttendanceListWithoutTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "scr060.targetMonth.required"))
                .thenReturn(
                        "対象年月を選択してください。");

        mockMvc.perform(
                        get("/api/attendances")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月を選択してください。"));

        verify(
                monthlyAttendanceListService,
                never())
                .getMonthlyAttendanceList(
                        any(),
                        any());
    }

    @Test
    void getMonthlyAttendanceListWithInvalidTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "error.attendance.targetMonth.invalid"))
                .thenReturn(
                        "対象年月の形式が正しくありません（yyyy-MM）。");

        mockMvc.perform(
                        get("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026/07")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月の形式が正しくありません（yyyy-MM）。"));

        verify(
                monthlyAttendanceListService,
                never())
                .getMonthlyAttendanceList(
                        any(),
                        any());
    }

    @Test
    void registerAttendanceReturnsCreated()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        AttendanceCreateRequest request =
                new AttendanceCreateRequest();

        request.setWorkDate("2026-07-22");
        request.setAttendanceTime("09:00");
        request.setLeavingTime("18:00");
        request.setWorkType("NORMAL");

        mockMvc.perform(
                        post("/api/attendances")
                                .principal(principal)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request)))
                .andExpect(status().isCreated());

        verify(attendanceRegistrationService)
                .register(
                        1L,
                        request);
    }

    @Test
    void updateAttendanceReturnsNoContent()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        AttendanceUpdateRequest request =
                new AttendanceUpdateRequest();

        request.setAttendanceTime("09:30");
        request.setLeavingTime("18:30");
        request.setWorkType("NORMAL");

        mockMvc.perform(
                        put("/api/attendances/2026-07-22")
                                .principal(principal)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request)))
                .andExpect(status().isNoContent());

        verify(attendanceUpdateService)
                .update(
                        1L,
                        LocalDate.of(2026, 7, 22),
                        request);
    }

    @Test
    void updateAttendanceWithInvalidWorkDateReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "error.attendance.workDate.invalid"))
                .thenReturn(
                        "勤務日の形式が正しくありません。");

        AttendanceUpdateRequest request =
                new AttendanceUpdateRequest();

        request.setAttendanceTime("09:30");
        request.setLeavingTime("18:30");
        request.setWorkType("NORMAL");

        mockMvc.perform(
                        put("/api/attendances/invalid-date")
                                .principal(principal)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "勤務日の形式が正しくありません。"));

        verify(
                attendanceUpdateService,
                never())
                .update(
                        any(),
                        any(),
                        any());
    }

    @Test
    void requestWithoutPrincipalReturnsUnauthorized()
            throws Exception {

        when(messageService.getMessage(
                "error.authentication.required"))
                .thenReturn(
                        "ログイン情報を確認できません。");

        mockMvc.perform(
                        get("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026-07"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "ログイン情報を確認できません。"));

        verify(
                employeeService,
                never())
                .getEmployeeDetail(any());
    }

    @Test
    void importAttendanceCsvReturnsSuccess()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "attendance.csv",
                        "text/csv",
                        """
                        employeeNo,workDate,attendanceTime,leavingTime,workType
                        1924,2026-07-01,09:00,18:00,NORMAL
                        """.getBytes());

        AttendanceCsvImportResponse response =
                new AttendanceCsvImportResponse(
                        true,
                        "勤怠CSVを取り込みました。読込件数：1、登録件数：1",
                        List.of());

        when(attendanceCsvImportService.importCsv(
                1L,
                "1924",
                YearMonth.of(2026, 7),
                file))
                .thenReturn(response);

        mockMvc.perform(
                        multipart(
                                "/api/attendances/csv-import")
                                .file(file)
                                .param(
                                        "targetMonth",
                                        "2026-07")
                                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.success")
                                .value(true))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "勤怠CSVを取り込みました。読込件数：1、登録件数：1"))
                .andExpect(
                        jsonPath("$.errors")
                                .isEmpty());

        verify(employeeService)
                .getEmployeeDetail("1924");

        verify(attendanceCsvImportService)
                .importCsv(
                        1L,
                        "1924",
                        YearMonth.of(2026, 7),
                        file);
    }

    @Test
    void importAttendanceCsvReturnsValidationErrors()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "attendance.csv",
                        "text/csv",
                        """
                        employeeNo,workDate,attendanceTime,leavingTime,workType
                        9999,2026-07-01,09:00,18:00,NORMAL
                        """.getBytes());

        CsvImportError error =
                new CsvImportError(
                        2,
                        "社員番号",
                        "9999",
                        "社員番号が一致しません。");

        AttendanceCsvImportResponse response =
                new AttendanceCsvImportResponse(
                        false,
                        "CSV内にエラーが存在します。",
                        List.of(error));

        when(attendanceCsvImportService.importCsv(
                1L,
                "1924",
                YearMonth.of(2026, 7),
                file))
                .thenReturn(response);

        mockMvc.perform(
                        multipart(
                                "/api/attendances/csv-import")
                                .file(file)
                                .param(
                                        "targetMonth",
                                        "2026-07")
                                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "CSV内にエラーが存在します。"))
                .andExpect(
                        jsonPath("$.errors[0].lineNumber")
                                .value(2))
                .andExpect(
                        jsonPath("$.errors[0].itemName")
                                .value("社員番号"))
                .andExpect(
                        jsonPath("$.errors[0].value")
                                .value("9999"))
                .andExpect(
                        jsonPath("$.errors[0].errorMessage")
                                .value(
                                        "社員番号が一致しません。"));

        verify(attendanceCsvImportService)
                .importCsv(
                        1L,
                        "1924",
                        YearMonth.of(2026, 7),
                        file);
    }

    @Test
    void importAttendanceCsvWithoutTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "scr070.targetMonth.required"))
                .thenReturn(
                        "取込対象年月を指定してください。");

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "attendance.csv",
                        "text/csv",
                        "header".getBytes());

        mockMvc.perform(
                        multipart(
                                "/api/attendances/csv-import")
                                .file(file)
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "取込対象年月を指定してください。"));

        verify(
                attendanceCsvImportService,
                never())
                .importCsv(
                        any(),
                        any(),
                        any(),
                        any());
    }

    @Test
    void importAttendanceCsvWithInvalidTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "error.attendance.targetMonth.invalid"))
                .thenReturn(
                        "対象年月の形式が正しくありません（yyyy-MM）。");

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "attendance.csv",
                        "text/csv",
                        "header".getBytes());

        mockMvc.perform(
                        multipart(
                                "/api/attendances/csv-import")
                                .file(file)
                                .param(
                                        "targetMonth",
                                        "2026/07")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月の形式が正しくありません（yyyy-MM）。"));

        verify(
                attendanceCsvImportService,
                never())
                .importCsv(
                        any(),
                        any(),
                        any(),
                        any());
    }
    /**
     * 勤怠CSVがダウンロードできること。
     */
    @Test
    void exportAttendanceCsvReturnsCsvFile()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        String csvContent =
                "\uFEFF"
                        + "employeeNo,workDate,"
                        + "attendanceTime,leavingTime,workType"
                        + System.lineSeparator()
                        + "1924,2026-07-01,"
                        + "09:00,18:00,NORMAL"
                        + System.lineSeparator();

        CsvFileData csvFile =
                new CsvFileData(
                        "勤怠_1924_2026-07.csv",
                        csvContent.getBytes(
                                StandardCharsets.UTF_8));

        when(attendanceCsvExportService.exportCsv(
                any(),
                any(),
                any()))
                .thenReturn(csvFile);

        mockMvc.perform(
                        get("/api/attendances/csv-export")
                                .param(
                                        "targetMonth",
                                        "2026-07")
                                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(
                        header()
                                .string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("attachment")))
                .andExpect(
                        header()
                                .string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        containsString("filename")))
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "text/csv"))
                .andExpect(
                        content()
                                .bytes(
                                        csvContent.getBytes(
                                                StandardCharsets.UTF_8)));

        verify(employeeService)
                .getEmployeeDetail("1924");

        verify(attendanceCsvExportService)
                .exportCsv(
                        1L,
                        "1924",
                        YearMonth.of(2026, 7));
    }

    /**
     * CSV出力対象年月が未指定の場合、
     * 400エラーになること。
     */
    @Test
    void exportAttendanceCsvWithoutTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "scr070.export.targetMonth.required"))
                .thenReturn(
                        "対象年月を選択してください。");

        mockMvc.perform(
                        get("/api/attendances/csv-export")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月を選択してください。"));

        verify(
                attendanceCsvExportService,
                never())
                .exportCsv(
                        any(),
                        any(),
                        any());
    }

    /**
     * CSV出力対象年月の形式が不正な場合、
     * 400エラーになること。
     */
    @Test
    void exportAttendanceCsvWithInvalidTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");

        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());

        when(messageService.getMessage(
                "error.attendance.targetMonth.invalid"))
                .thenReturn(
                        "対象年月の形式が正しくありません（yyyy-MM）。");

        mockMvc.perform(
                        get("/api/attendances/csv-export")
                                .param(
                                        "targetMonth",
                                        "2026/07")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月の形式が正しくありません（yyyy-MM）。"));

        verify(
                attendanceCsvExportService,
                never())
                .exportCsv(
                        any(),
                        any(),
                        any());
    }

    @Test
    void deleteMonthlyAttendancesReturnsDeleteResult()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");
        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());
        when(attendanceMonthlyDeleteService
                .deleteMonthlyAttendances(
                        1L,
                        YearMonth.of(2026, 7)))
                .thenReturn(
                        new AttendanceMonthlyDeleteResponse(
                                21,
                                "対象月の勤怠データを削除しました。"));

        mockMvc.perform(
                        delete("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026-07")
                                .principal(principal))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.deletedCount")
                                .value(21))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象月の勤怠データを削除しました。"));

        verify(attendanceMonthlyDeleteService)
                .deleteMonthlyAttendances(
                        1L,
                        YearMonth.of(2026, 7));
    }

    @Test
    void deleteMonthlyAttendancesWithInvalidTargetMonthReturnsBadRequest()
            throws Exception {

        when(principal.getName())
                .thenReturn("1924");
        when(employeeService.getEmployeeDetail("1924"))
                .thenReturn(createEmployeeDetail());
        when(messageService.getMessage(
                "error.attendance.targetMonth.invalid"))
                .thenReturn(
                        "対象年月の形式が正しくありません（yyyy-MM）。");

        mockMvc.perform(
                        delete("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026/07")
                                .principal(principal))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月の形式が正しくありません（yyyy-MM）。"));

        verify(
                attendanceMonthlyDeleteService,
                never())
                .deleteMonthlyAttendances(
                        any(),
                        any());
    }

    @Test
    void deleteMonthlyAttendancesWithoutPrincipalReturnsUnauthorized()
            throws Exception {

        when(messageService.getMessage(
                "error.authentication.required"))
                .thenReturn(
                        "認証が必要です。");

        mockMvc.perform(
                        delete("/api/attendances")
                                .param(
                                        "targetMonth",
                                        "2026-07"))
                .andExpect(status().isUnauthorized())
                .andExpect(
                        jsonPath("$.message")
                                .value("認証が必要です。"));

        verify(
                attendanceMonthlyDeleteService,
                never())
                .deleteMonthlyAttendances(
                        any(),
                        any());
    }

    private EmployeeDetailDTO createEmployeeDetail() {

        return new EmployeeDetailDTO(
                1L,
                "1924",
                "テスト社員",
                1L,
                LocalDate.of(1990, 1, 1),
                "1000001",
                "東京都",
                "09012345678",
                "test@example.com",
                LocalDate.of(2026, 4, 1),
                null,
                1L,
                1,
                List.of());
    }
}
