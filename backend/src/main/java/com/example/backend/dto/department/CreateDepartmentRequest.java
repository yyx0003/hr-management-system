package com.example.backend.dto.department;

import java.time.LocalDate;

public record CreateDepartmentRequest(
    String departmentName,
    LocalDate startDate
) {
}
