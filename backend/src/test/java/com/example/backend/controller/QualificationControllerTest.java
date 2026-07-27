package com.example.backend.controller;

import com.example.backend.dto.qualification.CreateQualificationRequest;
import com.example.backend.dto.qualification.UpdateQualificationRequest;
import com.example.backend.entity.Qualification;
import com.example.backend.service.QualificationService;
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
class QualificationControllerTest {

    @Mock
    private QualificationService qualificationService;

    @InjectMocks
    private QualificationController qualificationController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(
                            new JavaTimeModule());

    @BeforeEach
    void setUp() {

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        qualificationController)
                .build();
    }

    @Test
    @DisplayName("全履歴取得")
    void findAllTest() throws Exception {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(1L);
        qualification.setQualificationName(
                "基本情報技術者");
        qualification.setStartDate(
                LocalDate.of(2026, 4, 1));

        when(qualificationService.findAll())
                .thenReturn(
                        List.of(qualification));

        mockMvc.perform(get("/qualification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qualificationId")
                        .value(1))
                .andExpect(jsonPath("$[0].qualificationName")
                        .value("基本情報技術者"));
    }

    @Test
    @DisplayName("新規資格登録")
    void createQualificationTest() throws Exception {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(4L);
        qualification.setQualificationName(
                "応用情報技術者");
        qualification.setIsAdvance(false);;
        qualification.setQualificationAllowance(
                10000L);
        qualification.setStartDate(
                LocalDate.of(2027, 1, 1));

        when(qualificationService.createQualification(
                any(),
                any(),
                any(),
                any()))
                .thenReturn(qualification);

        CreateQualificationRequest request =
                new CreateQualificationRequest(
                        "応用情報技術者",
                        false,
                        10000L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(post("/qualification")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualificationId")
                        .value(4))
                .andExpect(jsonPath("$.qualificationName")
                        .value("応用情報技術者"));
    }

    @Test
    @DisplayName("資格履歴更新")
    void updateQualificationTest() throws Exception {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(1L);
        qualification.setQualificationName(
                "応用情報技術者");
        qualification.setStartDate(
                LocalDate.of(2026, 5, 1));

        when(qualificationService.updateQualification(
                any(),
                any(),
                any(),
                any(),
                any()))
                .thenReturn(qualification);

        UpdateQualificationRequest request =
                new UpdateQualificationRequest(
                        1L,
                        "応用情報技術者",
                        false,
                        12000L,
                        LocalDate.of(2026, 5, 1));

        mockMvc.perform(put("/qualification")
                .contentType(
                        MediaType.APPLICATION_JSON)
                .content(
                        objectMapper.writeValueAsString(
                                request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qualificationId")
                        .value(1))
                .andExpect(jsonPath("$.qualificationName")
                        .value("応用情報技術者"));
    }

    @Test
    @DisplayName("資格履歴削除")
    void deleteQualificationTest() throws Exception {

        doNothing().when(qualificationService)
                .deleteQualification(
                        1L,
                        LocalDate.of(2027, 1, 1));

        mockMvc.perform(delete("/qualification/{qualificationId}/{startDate}", 1L, "2027-01-01"))
                .andExpect(status().isOk());

        verify(qualificationService)
                .deleteQualification(
                        1L,
                        LocalDate.of(2027, 1, 1));
    }
}