package com.example.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.GlobalExceptionHandler;
import com.example.backend.dto.attendance.AttendanceCreateRequest;
import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.dto.attendance.AttendanceListResponse;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.dto.employee.EmployeeDetailDTO;
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
                                        objectMapper))
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
                        jsonPath("$.attendanceList[0].workDate")
                                .value("2026-07-01"))
                .andExpect(
                        jsonPath("$.attendanceList[0].attendanceTime")
                                .value("09:00"))
                .andExpect(
                        jsonPath("$.attendanceList[0].leavingTime")
                                .value("18:00"))
                .andExpect(
                        jsonPath("$.attendanceList[0].workType")
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
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
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
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
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
                        put("/api/attendances/2026-07-xx")
                                .principal(principal)
                                .contentType("application/json")
                                .content(
                                        objectMapper.writeValueAsString(
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