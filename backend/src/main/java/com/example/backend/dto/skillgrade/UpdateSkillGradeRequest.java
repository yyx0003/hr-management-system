package com.example.backend.dto.skillgrade;

import java.time.LocalDate;

public record UpdateSkillGradeRequest(
        Integer skillGrade,
        Long allowance,
        LocalDate startDate) {
}