package com.example.backend.dto.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 勤怠CSV解析结果。
 */
@Getter
@AllArgsConstructor
public class AttendanceCsvParseResult {

    /**
     * 読み込んだCSVデータ。
     */
    private final List<AttendanceCsvRow> rows;

    /**
     * CSV形式に関するエラー。
     */
    private final List<CsvImportError> errors;
}