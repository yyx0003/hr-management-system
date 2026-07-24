package com.example.backend.controller;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.common.exception.GlobalExceptionHandler;
import com.example.backend.dto.employee.EmployeeCreateResponse;
import com.example.backend.dto.employee.EmployeeDetailDTO;
import com.example.backend.dto.employee.QualificationDetailDTO;
import com.example.backend.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EmployeeController(employeeService, messageService))
                .setControllerAdvice(new GlobalExceptionHandler(messageService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        new ObjectMapper()
                                .registerModule(new JavaTimeModule())
                                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)))
                .build();
    }

    @Test
    void getEmployeeDetailReturnsAllResponseFields() throws Exception {
        when(employeeService.getEmployeeDetail("12345")).thenReturn(detail(List.of()));

        mockMvc.perform(get("/api/employees/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.employeeNo").value("12345"))
                .andExpect(jsonPath("$.employeeName").value("Taro Yamada"))
                .andExpect(jsonPath("$.departmentId").value(2))
                .andExpect(jsonPath("$.birthDate").value("1990-01-02"))
                .andExpect(jsonPath("$.postalCode").value("1000001"))
                .andExpect(jsonPath("$.address").value("Tokyo"))
                .andExpect(jsonPath("$.phoneNumber").value("09012345678"))
                .andExpect(jsonPath("$.emailAddress").value("taro@example.com"))
                .andExpect(jsonPath("$.hireDate").value("2020-04-01"))
                .andExpect(jsonPath("$.retireDate").doesNotExist())
                .andExpect(jsonPath("$.positionId").value(3))
                .andExpect(jsonPath("$.skillGrade").value(4))
                .andExpect(jsonPath("$.qualifications").isArray());
    }

    @Test
    void createEmployeeReturnsCreatedWithGeneratedIdentifiers() throws Exception {
        when(employeeService.createEmployee(any()))
                .thenReturn(new EmployeeCreateResponse(1L, "0001"));

        mockMvc.perform(post("/api/employees")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"employeeName":"Taro Yamada","birthDate":"1990-01-02","postalCode":"1000001","address":"Tokyo","hireDate":"2026-07-01","departmentId":1,"skillGrade":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(1))
                .andExpect(jsonPath("$.employeeNo").value("0001"));
    }

    @Test
    void createEmployeeReturnsBadRequestForBeanValidationError() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getEmployeeDetailReturnsEmptyArrayWhenNoQualificationsAreHeld() throws Exception {
        when(employeeService.getEmployeeDetail("12345")).thenReturn(detail(List.of()));

        mockMvc.perform(get("/api/employees/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualifications").isArray())
                .andExpect(jsonPath("$.qualifications").isEmpty());
    }

    @Test
    void getEmployeeDetailReturnsQualificationsAndAcquisitionDates() throws Exception {
        List<QualificationDetailDTO> qualifications = List.of(
                new QualificationDetailDTO(10L, LocalDate.of(2021, 5, 1)),
                new QualificationDetailDTO(20L, LocalDate.of(2022, 6, 2)));
        when(employeeService.getEmployeeDetail("12345")).thenReturn(detail(qualifications));

        mockMvc.perform(get("/api/employees/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualifications[0].qualificationId").value(10))
                .andExpect(jsonPath("$.qualifications[0].acquisitionDate").value("2021-05-01"))
                .andExpect(jsonPath("$.qualifications[1].qualificationId").value(20))
                .andExpect(jsonPath("$.qualifications[1].acquisitionDate").value("2022-06-02"));
    }

    @Test
    void getEmployeeDetailReturnsNotFoundWhenEmployeeDoesNotExist() throws Exception {
        when(employeeService.getEmployeeDetail("12345"))
                .thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "not found"));

        mockMvc.perform(get("/api/employees/12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("not found"));
    }

    @Test
    void getEmployeeDetailRejectsInvalidEmployeeNo() throws Exception {
        when(messageService.getMessage("scr040.employeeNo.format"))
                .thenReturn("invalid employee number");

        mockMvc.perform(get("/api/employees/E0001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("invalid employee number"));

        verify(employeeService, never()).getEmployeeDetail("E0001");
    }

    private EmployeeDetailDTO detail(List<QualificationDetailDTO> qualifications) {
        return new EmployeeDetailDTO(
                1L, "12345", "Taro Yamada", 2L,
                LocalDate.of(1990, 1, 2), "1000001", "Tokyo",
                "09012345678", "taro@example.com", LocalDate.of(2020, 4, 1),
                null, 3L, 4, qualifications);
    }
}
