package com.example.backend.controller;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.GlobalExceptionHandler;
import com.example.backend.dto.employee.EmployeeListDTO;
import com.example.backend.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmployeeSearchControllerTest {

    @Mock
    private EmployeeService employeeService;

    @Mock
    private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EmployeeController(employeeService, messageService))
                .setControllerAdvice(new GlobalExceptionHandler(messageService))
                .build();
    }

    @Test
    void searchReturnsAllEmployeesInServiceOrderWhenNoConditionsAreSpecified() throws Exception {
        when(employeeService.searchEmployees(null, null, null))
                .thenReturn(List.of(employee(1L, "0002", "B"), employee(2L, "0010", "A")));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeNo").value("0002"))
                .andExpect(jsonPath("$[1].employeeNo").value("0010"));

        verify(employeeService).searchEmployees(null, null, null);
    }

    @Test
    void searchPassesEmployeeNoPartialMatchCondition() throws Exception {
        when(employeeService.searchEmployees("12", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees").param("employeeNo", " 12 "))
                .andExpect(status().isOk());

        verify(employeeService).searchEmployees("12", null, null);
    }

    @Test
    void searchPassesEmployeeNamePartialMatchCondition() throws Exception {
        when(employeeService.searchEmployees(null, "Taro", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees").param("employeeName", " Taro "))
                .andExpect(status().isOk());

        verify(employeeService).searchEmployees(null, "Taro", null);
    }

    @Test
    void searchPassesDepartmentIdExactMatchCondition() throws Exception {
        when(employeeService.searchEmployees(null, null, 2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees").param("departmentId", "2"))
                .andExpect(status().isOk());

        verify(employeeService).searchEmployees(null, null, 2L);
    }

    @Test
    void searchCombinesAllConditionsWithAnd() throws Exception {
        when(employeeService.searchEmployees("12", "Taro", 2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees")
                        .param("employeeNo", "12")
                        .param("employeeName", "Taro")
                        .param("departmentId", "2"))
                .andExpect(status().isOk());

        verify(employeeService).searchEmployees("12", "Taro", 2L);
    }

    @Test
    void searchReturnsEmptyArrayWhenThereAreNoMatches() throws Exception {
        when(employeeService.searchEmployees(null, null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/employees")
                        .param("employeeNo", " ")
                        .param("employeeName", "　")
                        .param("departmentId", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void searchReturnsNullPositionNameForEmployeeWithoutPosition() throws Exception {
        when(employeeService.searchEmployees(null, null, null))
                .thenReturn(List.of(employee(1L, "0001", "Taro")));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].positionName").value(nullValue()));
    }

    @Test
    void searchRejectsInvalidEmployeeNo() throws Exception {
        invalidRequest("employeeNo", "E0001", "scr040.employeeNo.format");
        invalidRequest("employeeNo", "123456789012345678901", "scr040.employeeNo.format");
    }

    @Test
    void searchRejectsEmployeeNameLongerThanOneHundredCharacters() throws Exception {
        invalidRequest("employeeName", "a".repeat(101), "scr050.employeeName.maxlength");
    }

    @Test
    void searchRejectsInvalidDepartmentId() throws Exception {
        invalidRequest("departmentId", "abc", "scr050.departmentId.invalid");
        invalidRequest("departmentId", "0", "scr050.departmentId.invalid");
        invalidRequest("departmentId", "-1", "scr050.departmentId.invalid");
    }

    private void invalidRequest(String parameterName, String parameterValue, String messageCode) throws Exception {
        when(messageService.getMessage(messageCode)).thenReturn("invalid");

        mockMvc.perform(get("/api/employees").param(parameterName, parameterValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("invalid"));

        verifyNoInteractions(employeeService);
    }

    private EmployeeListDTO employee(Long employeeId, String employeeNo, String employeeName) {
        EmployeeListDTO employee = new EmployeeListDTO();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeNo(employeeNo);
        employee.setEmployeeName(employeeName);
        employee.setDepartmentName("Development");
        employee.setPositionName(null);
        employee.setSkillGrade(3);
        employee.setHireDate(LocalDate.of(2020, 4, 1));
        return employee;
    }
}
