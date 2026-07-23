package com.example.backend.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCsvParseResult;
import com.example.backend.dto.attendance.AttendanceCsvRow;
import com.example.backend.dto.attendance.CsvImportError;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠CSVを読み込むサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceCsvParserService {

    private static final int COLUMN_COUNT = 5;

    private static final String[] EXPECTED_HEADER = {
            "employeeNo",
            "workDate",
            "attendanceTime",
            "leavingTime",
            "workType"
    };

    private final MessageService messageService;

    /**
     * 勤怠CSVを読み込む。
     *
     * @param file CSVファイル
     * @return CSV解析結果
     */
    public AttendanceCsvParseResult parse(
            MultipartFile file) {

        validateFile(file);

        List<AttendanceCsvRow> rows =
                new ArrayList<>();

        List<CsvImportError> errors =
                new ArrayList<>();

        try (BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8))) {

            String headerLine =
                    reader.readLine();

            if (headerLine == null) {
                throw new BusinessException(
                        messageService.getMessage(
                                "scr070.csvFile.empty"));
            }

            validateHeader(
                    removeBom(headerLine));

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                parseLine(
                        line,
                        lineNumber,
                        rows,
                        errors);
            }

        } catch (IOException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "scr070.csvFile.readFailed"));
        }

        return new AttendanceCsvParseResult(
                rows,
                errors);
    }

    /**
     * ファイルが選択されていることを確認する。
     *
     * @param file CSVファイル
     */
    private void validateFile(
            MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    messageService.getMessage(
                            "scr070.csvFile.required"));
        }
    }

    /**
     * CSVヘッダーを確認する。
     *
     * @param headerLine ヘッダー行
     */
    private void validateHeader(
            String headerLine) {

        String[] columns =
                headerLine.split(",", -1);

        if (columns.length != EXPECTED_HEADER.length) {
            throw new BusinessException(
                    messageService.getMessage(
                            "scr070.csvHeader.invalid"));
        }

        for (int index = 0;
                index < EXPECTED_HEADER.length;
                index++) {

            if (!EXPECTED_HEADER[index]
                    .equals(columns[index].trim())) {

                throw new BusinessException(
                        messageService.getMessage(
                                "scr070.csvHeader.invalid"));
            }
        }
    }

    /**
     * CSVの1行を読み込む。
     *
     * @param line CSV行
     * @param lineNumber 行番号
     * @param rows 正常に読み込んだ行
     * @param errors エラー一覧
     */
    private void parseLine(
            String line,
            int lineNumber,
            List<AttendanceCsvRow> rows,
            List<CsvImportError> errors) {

        String[] columns =
                line.split(",", -1);

        if (columns.length != COLUMN_COUNT) {
            errors.add(
                    new CsvImportError(
                            lineNumber,
                            "CSV",
                            line,
                            messageService.getMessage(
                                    "scr070.csvColumn.invalid")));

            return;
        }

        AttendanceCsvRow row =
                new AttendanceCsvRow(
                        lineNumber,
                        columns[0].trim(),
                        columns[1].trim(),
                        columns[2].trim(),
                        columns[3].trim(),
                        columns[4].trim());

        rows.add(row);
    }

    /**
     * UTF-8 BOMを削除する。
     *
     * @param value 文字列
     * @return BOM削除後の文字列
     */
    private String removeBom(
            String value) {

        if (value != null
                && value.startsWith("\uFEFF")) {

            return value.substring(1);
        }

        return value;
    }
}