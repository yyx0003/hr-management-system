package com.example.backend.dto.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record EmployeeQualificationCreateRequest(
        @NotNull(message = "{scr030.qualificationId.required}") Long qualificationId,
        @NotNull(message = "{scr030.acquisitionDate.required}") LocalDate acquisitionDate) {
}
