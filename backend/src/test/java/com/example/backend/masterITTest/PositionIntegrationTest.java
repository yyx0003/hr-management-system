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

import com.example.backend.dto.position.CreatePositionRequest;
import com.example.backend.dto.position.UpdatePositionRequest;
import com.example.backend.entity.Position;
import com.example.backend.repository.PositionRepository;
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
class PositionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PositionRepository positionRepository;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        String response =
                mockMvc.perform(get("/position"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<Position> positions =
                objectMapper.readValue(
                        response,
                        new TypeReference<List<Position>>() {
                        });

        assertThat(positions)
                .hasSize(12);

        assertThat(positions)
                .extracting(Position::getPositionName)
                .containsExactly(
                        "主任",
                        "係長",
                        "係長",
                        "係長",
                        "課長",
                        "課長",
                        "課長",
                        "部長",
                        "部長",
                        "部長",
                        "削除失敗用",
                        "削除失敗用");
    }

    @Test
    @DisplayName("新規役職登録")
    void createPositionTest() throws Exception {

        CreatePositionRequest request =
                new CreatePositionRequest(
                        "本部長",
                        80000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionName")
                        .value("本部長"));

        List<Position> positions =
                positionRepository.selectList(null);

        assertThat(positions)
                .extracting(
                        Position::getPositionId,
                        Position::getPositionName)
                .contains(
                        tuple(6L, "本部長"));
    }

    @Test
    @DisplayName("役職履歴更新")
    void updatePositionTest() throws Exception {

        UpdatePositionRequest request =
                new UpdatePositionRequest(
                        1L,
                        "上級主任",
                        15000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(put("/position")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionName")
                        .value("上級主任"));

        List<Position> positions =
                positionRepository.selectList(null);

        assertThat(positions)
                .extracting(
                        Position::getPositionName,
                        Position::getEndDate)
                .contains(
                        tuple(
                                "主任",
                                LocalDate.of(2026, 12, 31)));

        Position latest =
                positionRepository.findLatestByPositionId(
                        1L);

        assertThat(latest.getPositionName())
                .isEqualTo("上級主任");

        assertThat(latest.getPositionAllowance())
                .isEqualTo(15000L);

        assertThat(latest.getStartDate())
                .isEqualTo(
                        LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("廃止予定役職は更新できない")
    void updatePositionTest2() throws Exception {

        UpdatePositionRequest request =
                new UpdatePositionRequest(
                        3L,
                        "新課長",
                        65000L,
                        LocalDate.of(2026, 7, 1));

        mockMvc.perform(put("/position")
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
    void updatePositionTest3() throws Exception {

        UpdatePositionRequest request =
                new UpdatePositionRequest(
                        2L,
                        "新係長",
                        35000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/position")
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
    @DisplayName("役職履歴削除")
    void deletePositionTest1() throws Exception {

        mockMvc.perform(
                delete(
                        "/position/{positionId}/{startDate}",
                        4L,
                        "2026-11-01"))
                .andExpect(status().isOk());

        Position result =
                positionRepository.findEffectiveAt(
                        4L,
                        LocalDate.of(2026, 11, 1));

        assertThat(result)
                .isNull();
    }

    @Test
    @DisplayName("過去の履歴は削除できない")
    void deletePositionTest2() throws Exception {

        mockMvc.perform(
                delete(
                        "/position/{positionId}/{startDate}",
                        4L,
                        "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "過去の履歴データ、または現在有効なデータは削除できません。"));

        Position result =
                positionRepository.findEffectiveAt(
                        4L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result)
                .isNotNull();
    }

    @Test
    @DisplayName("IDが他エンティティに紐づいている場合は削除できない")
    void deletePositionTest3() throws Exception {

        mockMvc.perform(
                delete(
                        "/position/{positionId}/{startDate}",
                        5L,
                        "2026-11-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(
                                "業務データから参照されているため削除できません。"));

        Position result =
                positionRepository.findEffectiveAt(
                        5L,
                        LocalDate.of(2026, 11, 1));

        assertThat(result)
                .isNotNull();
    }
}