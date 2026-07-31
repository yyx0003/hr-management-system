package com.example.backend.masterITTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.backend.dto.department.CreateDepartmentRequest;
import com.example.backend.dto.department.UpdateDepartmentRequest;
import com.example.backend.entity.Department;
import com.example.backend.repository.DepartmentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Sql(scripts = "/sql/integration-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class DepartmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentRepository departmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(
                    new JavaTimeModule());

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        String response = mockMvc.perform(get("/department"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Department> departments = objectMapper.readValue(response,
                new TypeReference<List<Department>>() {
                });
        assertThat(departments).hasSize(12);
        assertThat(departments)
                .extracting(Department::getDepartmentName)
                .containsExactly(
                        "経営",
                        "営業",
                        "AI推進部",
                        "DX推進部",
                        "開発１室",
                        "A社開発室",
                        "A社保守部",
                        "B社開発室",
                        "C社開発室",
                        "D社開発室",
                        "削除失敗用",
                        "削除失敗用");
    }

    @Test
    @DisplayName("新規部署登録")
    void createDepartmentTest() throws Exception {

        CreateDepartmentRequest request = new CreateDepartmentRequest(
                "E社開発室",
                LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName")
                        .value("E社開発室"));

        List<Department> departments = departmentRepository.selectList(null);

        assertThat(departments)
                .extracting(
                        Department::getDepartmentId,
                        Department::getDepartmentName)
                .contains(tuple(6L, "E社開発室"));
    }

    @Test
    @DisplayName("部署履歴更新")
    void updateDepartmentTest() throws Exception {

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                1L,
                "新経営部",
                LocalDate.of(2027, 1, 1));

        mockMvc.perform(put("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName")
                        .value("新経営部"));

        List<Department> departments = departmentRepository.selectList(null);
        // 切り替え前日に終了日が設定されているかを検証
        assertThat(departments)
                .extracting(Department::getDepartmentName,
                        Department::getEndDate)
                .contains(tuple("経営",
                        LocalDate.of(2026, 12, 31)));

        Department latest = departmentRepository.findLatestByDepartmentId(1L);

        assertThat(latest.getDepartmentName())
                .isEqualTo("新経営部");

        assertThat(latest.getStartDate())
                .isEqualTo(
                        LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("廃止予定部署は更新できない")
    void updateDepartmentTest2() throws Exception {

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                3L,
                "B社開発室",
                LocalDate.of(2026, 7, 1));

        mockMvc.perform(put("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value("無効化される予定のデータは削除できません。"));
    }

    @Test
    @DisplayName("最新履歴以前の開始日は設定できない")
    void updateDepartmentTest3() throws Exception {

        UpdateDepartmentRequest request = new UpdateDepartmentRequest(
                2L,
                "新DX推進部",
                LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/department")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value("新しい適用開始日は、現在有効なデータの適用終了日より後の日付にしてください。"));
    }

    @Test
    @DisplayName("部署履歴削除")
    void deleteDepartmentTest1() throws Exception {
        mockMvc.perform(delete("/department/{departmentId}/{startDate}", 4L, "2026-11-01"))
                .andExpect(status().isOk());

        Department result = departmentRepository.selectOne(
                Wrappers.lambdaQuery(Department.class).eq(Department::getDepartmentId, 4L)
                .eq(Department::getStartDate, LocalDate.of(2026,11,1))       
        );
        
        assertThat(result)
                .isNull();
    }

    @Test
    @DisplayName("過去の履歴は削除できない")
    void deleteDepartmentTest2() throws Exception {

        mockMvc.perform(delete("/department/{departmentId}/{startDate}", 4L, "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("過去の履歴データ、または現在有効なデータは削除できません。"));

        Department result = departmentRepository.findEffectiveAt(
                4L,
                LocalDate.of(2026, 5, 1));

        assertThat(result)
                .isNotNull();
    }

    @Test
    @DisplayName("IDが他エンティティに紐づいている場合は削除できない")
    void deleteDepartmentTest3() throws Exception {

        mockMvc.perform(delete("/department/{departmentId}/{startDate}", 5L, "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("業務データから参照されているため削除できません。"));

        Department result = departmentRepository.findEffectiveAt(
                5L,
                LocalDate.of(2026, 11, 1));

        assertThat(result)
                .isNotNull();
    }
}