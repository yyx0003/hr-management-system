package com.example.backend.dto.qualification;

import java.time.LocalDate;

public record UpdateQualificationRequest(
        Long qualificationId,
        String qualificationName,
        Boolean isAdvance,
        Long qualificationAllowance,
        LocalDate startDate) {
}