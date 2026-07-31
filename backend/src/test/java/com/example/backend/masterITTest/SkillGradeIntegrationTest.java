package com.example.backend.masterITTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.backend.dto.skillgrade.UpdateSkillGradeRequest;
import com.example.backend.entity.SkillGrade;
import com.example.backend.repository.SkillGradeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
@Sql(
        scripts = "/sql/integration-test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SkillGradeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SkillGradeRepository skillGradeRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        String response =
                mockMvc.perform(get("/skillgrade"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<SkillGrade> skillGrades =
                objectMapper.readValue(
                        response,
                        new TypeReference<List<SkillGrade>>() {
                        });

        assertThat(skillGrades)
                .hasSize(16);

        assertThat(skillGrades)
                .extracting(SkillGrade::getSkillGrade)
                .containsExactly(
                        1,
                        2, 2, 2,
                        3, 3, 3, 3,
                        4, 4,
                        5,
                        6,
                        7,
                        8,
                        9,
                        10);
    }

    @Test
    @DisplayName("職能資格履歴更新")
    void updateSkillGradeTest() throws Exception {

        UpdateSkillGradeRequest request =
                new UpdateSkillGradeRequest(
                        1,
                        15000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(put("/skillgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillGrade")
                        .value(1));

        List<SkillGrade> skillGrades =
                skillGradeRepository.selectList(null);

        assertThat(skillGrades)
                .extracting(
                        SkillGrade::getSkillGrade,
                        SkillGrade::getEndDate)
                .contains(
                        tuple(
                                1,
                                LocalDate.of(2026, 12, 31)));

        SkillGrade latest =
                skillGradeRepository
                        .findLatestBySkillGrade(1);

        assertThat(latest.getAllowance())
                .isEqualTo(15000L);

        assertThat(latest.getStartDate())
                .isEqualTo(
                        LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("廃止予定職能資格は更新できない")
    void updateSkillGradeTest2() throws Exception {

        UpdateSkillGradeRequest request =
                new UpdateSkillGradeRequest(
                        4,
                        45000L,
                        LocalDate.of(2026, 1, 1));

        mockMvc.perform(put("/skillgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value(
                                "無効化される予定のデータは削除できません。"));
    }

    @Test
    @DisplayName("最新履歴以前の開始日は設定できない")
    void updateSkillGradeTest3() throws Exception {

        UpdateSkillGradeRequest request =
                new UpdateSkillGradeRequest(
                        2,
                        35000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/skillgrade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success")
                        .value(false))
                .andExpect(jsonPath("$.message")
                        .value(
                                "新しい適用開始日は、現在有効なデータの適用終了日より後の日付にしてください。"));
    }

    @Test
    @DisplayName("職能資格履歴削除")
    void deleteSkillGradeTest1() throws Exception {

        mockMvc.perform(
                delete(
                        "/skillgrade/{skillGrade}/{startDate}",
                        2,
                        "2026-09-01"))
                .andExpect(status().isOk());

        SkillGrade result =
                skillGradeRepository.selectOne(Wrappers.<SkillGrade>lambdaQuery()
                        .eq(SkillGrade::getSkillGrade, 2)
                        .eq(SkillGrade::getStartDate, LocalDate.of(2026, 9, 1)));

        assertThat(result)
                .isNull();
    }

    @Test
    @DisplayName("過去の履歴は削除できない")
    void deleteSkillGradeTest2() throws Exception {

        mockMvc.perform(
                delete(
                        "/skillgrade/{skillGrade}/{startDate}",
                        4,
                        "2026-04-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "過去の履歴データ、または現在有効なデータは削除できません。"));

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        3,
                        LocalDate.of(2026, 6, 1));

        assertThat(result)
                .isNotNull();
    }

    @Test
    @DisplayName("IDが他エンティティに紐づいている場合は削除できない")
    void deleteSkillGradeTest3() throws Exception {

        mockMvc.perform(
                delete(
                        "/skillgrade/{skillGrade}/{startDate}",
                        3,
                        "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "業務データから参照されているため削除できません。"));

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        3,
                        LocalDate.of(2026, 9, 1));

        assertThat(result)
                .isNotNull();
    }
}