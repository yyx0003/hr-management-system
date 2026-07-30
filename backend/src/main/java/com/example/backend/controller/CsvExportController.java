package com.example.backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.csv.CsvExportRequest;
import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.service.CsvExportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 人事向け・経営向けCSV出力API。
 */
@RestController
@RequestMapping("/api/csv")
@RequiredArgsConstructor
public class CsvExportController {

    private final CsvExportService csvExportService;

    /**
     * 人事向けCSVを出力する。
     *
     * @param request CSV出力条件
     * @return CSVファイル
     */
    @PostMapping(
            value = "/hr/export",
            produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportHrCsv(
            @Valid @RequestBody
                    CsvExportRequest request) {

        CsvFileData csvFile =
                csvExportService.exportHrCsv(
                        YearMonth.parse(
                                request.getTargetMonth()));

        return createCsvResponse(csvFile);
    }

    /**
     * 経営向けCSVを出力する。
     *
     * @param request CSV出力条件
     * @return CSVファイル
     */
    @PostMapping(
            value = "/management/export",
            produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> exportManagementCsv(
            @Valid @RequestBody
                    CsvExportRequest request) {

        CsvFileData csvFile =
                csvExportService.exportManagementCsv(
                        YearMonth.parse(
                                request.getTargetMonth()));

        return createCsvResponse(csvFile);
    }

    private ResponseEntity<byte[]> createCsvResponse(
            CsvFileData csvFile) {

        HttpHeaders headers =
                new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                csvFile.getFileName(),
                                StandardCharsets.UTF_8)
                        .build());
        headers.setContentType(
                new MediaType(
                        "text",
                        "csv",
                        StandardCharsets.UTF_8));
        headers.setContentLength(
                csvFile.getContent().length);

        return new ResponseEntity<>(
                csvFile.getContent(),
                headers,
                HttpStatus.OK);
    }
}
