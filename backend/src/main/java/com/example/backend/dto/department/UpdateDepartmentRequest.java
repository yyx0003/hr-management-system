package com.example.backend.dto.department;

import java.time.LocalDate;

public record UpdateDepartmentRequest (
    Long departmentId,
    String departmentName,
    LocalDate startDate
) {
}
