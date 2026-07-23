package com.example.backend.dto.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 勤怠CSV校验结果。
 */
@Getter
@AllArgsConstructor
public class AttendanceCsvValidationResult {

    /**
     * 校验成功的数据。
     */
    private final List<AttendanceCsvValidatedRow> rows;

    /**
     * 校验错误列表。
     */
    private final List<CsvImportError> errors;

    /**
     * 是否存在错误。
     *
     * @return 存在错误时为true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}