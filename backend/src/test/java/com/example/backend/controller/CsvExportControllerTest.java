package com.example.backend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.service.CsvExportService;

@SpringBootTest(
        properties =
                "jwt.secret="
                        + "MDEyMzQ1Njc4OWFiY2RlZjAx"
                        + "MjM0NTY3ODlhYmNkZWY=")
@AutoConfigureMockMvc
class CsvExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CsvExportService csvExportService;

    private String authorization;

    @BeforeEach
    void setUp() {

        authorization =
                "Bearer "
                        + jwtTokenProvider.createToken(
                                1L,
                                "E0001");
    }

    @Test
    void exportHrCsvReturnsDownloadForAuthenticatedUser()
            throws Exception {

        byte[] content =
                "\uFEFF社員番号\r\n"
                        .getBytes(StandardCharsets.UTF_8);
        when(csvExportService.exportHrCsv(
                YearMonth.of(2026, 7)))
                .thenReturn(
                        new CsvFileData(
                                "人事向け_勤怠給与_202607.csv",
                                content));

        mockMvc.perform(
                        post("/api/csv/hr/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026-07"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(
                        content().contentType(
                                "text/csv;charset=UTF-8"))
                .andExpect(
                        content().bytes(content))
                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                containsString(
                                        "filename*=UTF-8''")));

        verify(csvExportService).exportHrCsv(
                YearMonth.of(2026, 7));
    }

    @Test
    void exportManagementCsvReturnsDownloadWithoutRoleRestriction()
            throws Exception {

        byte[] content =
                "\uFEFF部署名\r\n"
                        .getBytes(StandardCharsets.UTF_8);
        when(csvExportService.exportManagementCsv(
                YearMonth.of(2026, 7)))
                .thenReturn(
                        new CsvFileData(
                                "経営向け_部署別集計_202607.csv",
                                content));

        mockMvc.perform(
                        post("/api/csv/management/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026-07"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(
                        content().contentType(
                                "text/csv;charset=UTF-8"))
                .andExpect(
                        content().bytes(content))
                .andExpect(
                        header().string(
                                HttpHeaders.CONTENT_DISPOSITION,
                                containsString(
                                        "filename*=UTF-8''")));

        verify(csvExportService)
                .exportManagementCsv(
                        YearMonth.of(2026, 7));
    }

    @Test
    void exportRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                        post("/api/csv/hr/export")
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026-07"}
                                        """))
                .andExpect(status().isUnauthorized());

        verify(csvExportService, never())
                .exportHrCsv(any());
    }

    @Test
    void exportRejectsBlankTargetMonth()
            throws Exception {

        mockMvc.perform(
                        post("/api/csv/hr/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":""}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "出力対象年月を指定してください。"));

        verify(csvExportService, never())
                .exportHrCsv(any());
    }

    @Test
    void exportRejectsSlashSeparatedTargetMonth()
            throws Exception {

        mockMvc.perform(
                        post("/api/csv/hr/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026/07"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false));

        verify(csvExportService, never())
                .exportHrCsv(any());
    }

    @Test
    void exportRejectsInvalidMonth()
            throws Exception {

        mockMvc.perform(
                        post("/api/csv/management/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026-13"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false));

        verify(csvExportService, never())
                .exportManagementCsv(any());
    }

    @Test
    void exportReturnsBusinessErrorWhenNoSalaryResultExists()
            throws Exception {

        when(csvExportService.exportHrCsv(
                YearMonth.of(2026, 7)))
                .thenThrow(
                        new BusinessException(
                                "対象年月の給与実績データが存在しません。"));

        mockMvc.perform(
                        post("/api/csv/hr/export")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        authorization)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"targetMonth":"2026-07"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success")
                                .value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "対象年月の給与実績データが存在しません。"));
    }
}
