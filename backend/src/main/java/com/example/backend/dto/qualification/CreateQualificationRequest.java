package com.example.backend.dto.qualification;

import java.time.LocalDate;

public record CreateQualificationRequest(
        String qualificationName,
        Boolean isAdvance,
        Long qualificationAllowance,
        LocalDate startDate) {
}