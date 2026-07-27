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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.dto.qualification.CreateQualificationRequest;
import com.example.backend.dto.qualification.UpdateQualificationRequest;
import com.example.backend.entity.Qualification;
import com.example.backend.repository.QualificationRepository;
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
class QualificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QualificationRepository qualificationRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        String response =
                mockMvc.perform(get("/qualification"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Qualification> qualifications =
                objectMapper.readValue(
                        response,
                        new TypeReference<List<Qualification>>() {
                        });

        assertThat(qualifications)
                .hasSize(12);

        assertThat(qualifications)
                .extracting(
                        Qualification::getQualificationName)
                .containsExactly(
                        "基本情報技術者",
                        "応用情報技術者",
                        "応用情報技術者",
                        "応用情報技術者",
                        "プロジェクトマネージャ",
                        "プロジェクトマネージャ",
                        "プロジェクトマネージャ",
                        "資格4",
                        "資格4",
                        "資格4",
                        "削除失敗用",
                        "削除失敗用");
    }

    @Test
    @DisplayName("新規資格登録")
    void createQualificationTest() throws Exception {

        CreateQualificationRequest request =
                new CreateQualificationRequest(
                        "ネットワークスペシャリスト",
                        true,
                        80000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/qualification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualificationName")
                        .value("ネットワークスペシャリスト"));

        List<Qualification> qualifications =
                qualificationRepository.selectList(null);

        assertThat(qualifications)
                .extracting(
                        Qualification::getQualificationId,
                        Qualification::getQualificationName)
                .contains(
                        tuple(6L,
                                "ネットワークスペシャリスト"));
    }

    @Test
    @DisplayName("資格履歴更新")
    void updateQualificationTest() throws Exception {

        UpdateQualificationRequest request =
                new UpdateQualificationRequest(
                        1L,
                        "上級基本情報技術者",
                        true,
                        10000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(put("/qualification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualificationName")
                        .value("上級基本情報技術者"));

        List<Qualification> qualifications =
                qualificationRepository.selectList(null);

        assertThat(qualifications)
                .extracting(
                        Qualification::getQualificationName,
                        Qualification::getEndDate)
                .contains(
                        tuple(
                                "基本情報技術者",
                                LocalDate.of(2026, 12, 31)));

        Qualification latest =
                qualificationRepository
                        .findLatestByQualificationId(
                                1L);

        assertThat(latest.getQualificationName())
                .isEqualTo(
                        "上級基本情報技術者");

        assertThat(latest.getQualificationAllowance())
                .isEqualTo(10000L);

        assertThat(latest.getStartDate())
                .isEqualTo(
                        LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("廃止予定資格は更新できない")
    void updateQualificationTest2()
            throws Exception {

        UpdateQualificationRequest request =
                new UpdateQualificationRequest(
                        3L,
                        "新資格",
                        true,
                        40000L,
                        LocalDate.of(2026, 7, 1));

        mockMvc.perform(put("/qualification")
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
    void updateQualificationTest3()
            throws Exception {

        UpdateQualificationRequest request =
                new UpdateQualificationRequest(
                        2L,
                        "応用情報技術者",
                        false,
                        20000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/qualification")
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
    @DisplayName("資格履歴削除")
    void deleteQualificationTest1()
            throws Exception {

        mockMvc.perform(
                delete(
                        "/qualification/{qualificationId}/{startDate}",
                        4L,
                        "2026-11-01"))
                .andExpect(status().isOk());

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        4L,
                        LocalDate.of(2026, 11, 1));

        assertThat(result)
                .isNull();
    }

    @Test
    @DisplayName("過去の履歴は削除できない")
    void deleteQualificationTest2()
            throws Exception {

        mockMvc.perform(
                delete(
                        "/qualification/{qualificationId}/{startDate}",
                        4L,
                        "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "過去の履歴データ、または現在有効なデータは削除できません。"));

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        4L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result)
                .isNotNull();
    }

    @Test
    @DisplayName("IDが他エンティティに紐づいている場合は削除できない")
    void deleteQualificationTest3()
            throws Exception {

        mockMvc.perform(
                delete(
                        "/qualification/{qualificationId}/{startDate}",
                        5L,
                        "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "業務データから参照されているため削除できません。"));

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        5L,
                        LocalDate.of(2026, 11, 1));

        assertThat(result)
                .isNotNull();
    }
}