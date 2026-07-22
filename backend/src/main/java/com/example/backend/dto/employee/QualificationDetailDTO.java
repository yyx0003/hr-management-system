package com.example.backend.dto.employee;

import java.time.LocalDate;

public record QualificationDetailDTO(
        Long qualificationId,
        LocalDate acquisitionDate
) {
}
