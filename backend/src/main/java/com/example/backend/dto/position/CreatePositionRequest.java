package com.example.backend.dto.position;

import java.time.LocalDate;

public record CreatePositionRequest(
        String positionName,
        Long positionAllowance,
        LocalDate startDate) {
}