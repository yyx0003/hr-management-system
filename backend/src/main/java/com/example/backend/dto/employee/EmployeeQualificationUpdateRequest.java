package com.example.backend.dto.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** 社員更新時の保有資格入力。登録画面とは独立したメッセージキーを使用する。 */
public record EmployeeQualificationUpdateRequest(
        @NotNull(message = "{scr040.qualificationId.required}") Long qualificationId,
        @NotNull(message = "{scr040.acquisitionDate.required}") LocalDate acquisitionDate) {
}
