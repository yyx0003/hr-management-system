package com.example.backend.controller;

import com.example.backend.dto.skillgrade.UpdateSkillGradeRequest;
import com.example.backend.entity.SkillGrade;
import com.example.backend.service.SkillGradeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SkillGradeControllerTest {

    @Mock
    private SkillGradeService skillGradeService;

    @InjectMocks
    private SkillGradeController skillGradeController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        skillGradeController)
                .build();
    }

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        SkillGrade skillGrade =
                new SkillGrade();

        skillGrade.setSkillGrade(1);
        skillGrade.setAllowance(10000L);
        skillGrade.setStartDate(
                LocalDate.of(2026, 4, 1));

        when(skillGradeService.findAll())
                .thenReturn(
                        List.of(skillGrade));

        mockMvc.perform(get("/skillgrade"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skillGrade")
                        .value(1))
                .andExpect(jsonPath("$[0].allowance")
                        .value(10000));
    }

    @Test
    @DisplayName("職能資格履歴更新")
    void updateSkillGradeTest()
            throws Exception {

        SkillGrade skillGrade =
                new SkillGrade();

        skillGrade.setSkillGrade(1);
        skillGrade.setAllowance(20000L);
        skillGrade.setStartDate(
                LocalDate.of(2026, 5, 1));

        when(skillGradeService.updateSkillGrade(
                any(),
                any(),
                any()))
                .thenReturn(skillGrade);

        UpdateSkillGradeRequest request =
                new UpdateSkillGradeRequest(
                        1,
                        20000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/skillgrade")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillGrade")
                        .value(1))
                .andExpect(jsonPath("$.allowance")
                        .value(20000));
    }

    @Test
    @DisplayName("職能資格履歴削除")
    void deleteSkillGradeTest()
            throws Exception {

        doNothing().when(skillGradeService)
                .deleteSkillGrade(
                        1,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(delete("/skillgrade/{skillGrade}/{startDate}", 1L, "2027-01-01"))
                .andExpect(status().isOk());

        verify(skillGradeService)
                .deleteSkillGrade(
                        1,
                        LocalDate.of(2027, 1, 1));
    }
}