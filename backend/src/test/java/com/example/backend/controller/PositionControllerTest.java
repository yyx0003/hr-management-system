package com.example.backend.controller;

import com.example.backend.dto.position.CreatePositionRequest;
import com.example.backend.dto.position.UpdatePositionRequest;
import com.example.backend.entity.Position;
import com.example.backend.service.PositionService;
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
class PositionControllerTest {

    @Mock
    private PositionService positionService;

    @InjectMocks
    private PositionController positionController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        positionController)
                .build();
    }

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        Position position = new Position();

        position.setPositionId(1L);
        position.setPositionName("主任");
        position.setPositionAllowance(10000L);
        position.setStartDate(
                LocalDate.of(2026, 4, 1));

        when(positionService.findAll())
                .thenReturn(
                        List.of(position));

        mockMvc.perform(get("/position"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].positionId")
                        .value(1))
                .andExpect(jsonPath("$[0].positionName")
                        .value("主任"));
    }

    @Test
    @DisplayName("新規役職登録")
    void createPositionTest() throws Exception {

        Position position = new Position();

        position.setPositionId(4L);
        position.setPositionName("係長");
        position.setPositionAllowance(20000L);
        position.setStartDate(
                LocalDate.of(2027, 1, 1));

        when(positionService.createPosition(
                any(),
                any(),
                any()))
                .thenReturn(position);

        CreatePositionRequest request =
                new CreatePositionRequest(
                        "係長",
                        20000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/position")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId")
                        .value(4))
                .andExpect(jsonPath("$.positionName")
                        .value("係長"));
    }

    @Test
    @DisplayName("役職履歴更新")
    void updatePositionTest() throws Exception {

        Position position = new Position();

        position.setPositionId(1L);
        position.setPositionName("係長");
        position.setPositionAllowance(20000L);
        position.setStartDate(
                LocalDate.of(2026, 5, 1));

        when(positionService.updatePosition(
                any(),
                any(),
                any(),
                any()))
                .thenReturn(position);

        UpdatePositionRequest request =
                new UpdatePositionRequest(
                        1L,
                        "係長",
                        20000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/position")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positionId")
                        .value(1))
                .andExpect(jsonPath("$.positionName")
                        .value("係長"));
    }

    @Test
    @DisplayName("役職履歴削除")
    void deletePositionTest() throws Exception {

        doNothing().when(positionService)
                .deletePosition(
                        1L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(delete("/position/{positionId}/{startDate}", 1L, "2027-01-01"))
                .andExpect(status().isOk());

        verify(positionService)
                .deletePosition(
                        1L,
                        LocalDate.of(2027, 1, 1));
    }
}