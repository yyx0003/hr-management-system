package com.example.backend.controller;

import com.example.backend.dto.department.CreateDepartmentRequest;
import com.example.backend.dto.department.UpdateDepartmentRequest;
import com.example.backend.entity.Department;
import com.example.backend.service.DepartmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DepartmentControllerTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        departmentController)
                .build();
    }

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        Department department = new Department();

        department.setDepartmentId(1L);
        department.setDepartmentName("経営");
        department.setStartDate(
                LocalDate.of(2026, 4, 1));

        when(departmentService.findAll())
                .thenReturn(List.of(department));

        mockMvc.perform(get("/department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].departmentId")
                        .value(1))
                .andExpect(jsonPath("$[0].departmentName")
                        .value("経営"));
    }

    @Test
    @DisplayName("新規部署登録")
    void createDepartmentTest() throws Exception {

        Department department = new Department();

        department.setDepartmentId(4L);
        department.setDepartmentName("AI推進部");
        department.setStartDate(
                LocalDate.of(2027, 1, 1));

        when(departmentService.createDepartment(
                any(),
                any()))
                .thenReturn(department);

        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "AI推進部",
                LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId")
                        .value(4))
                .andExpect(jsonPath("$.departmentName")
                        .value("AI推進部"));
    }

    @Test
    @DisplayName("部署履歴更新")
    void updateDepartmentTest() throws Exception {

        Department department = new Department();

        department.setDepartmentId(1L);
        department.setDepartmentName("新営業部");
        department.setStartDate(
                LocalDate.of(2026, 5, 1));

        when(departmentService.updateDepartment(
                any(),
                any(),
                any()))
                .thenReturn(department);

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                1L,
                "新営業部",
                LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId")
                        .value(1))
                .andExpect(jsonPath("$.departmentName")
                        .value("新営業部"));
    }

    @Test
    @DisplayName("部署履歴削除")
    void deleteDepartmentTest() throws Exception {

        doNothing().when(departmentService)
                .deleteDepartment(
                        1L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(delete("/department/{departmentId}/{startDate}", 1L, "2027-01-01"))
                .andExpect(status().isOk());

        verify(departmentService)
                .deleteDepartment(
                        1L,
                        LocalDate.of(2027, 1, 1));
    }
}