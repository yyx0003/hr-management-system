package com.example.backend.dto.position;

import java.time.LocalDate;

public record UpdatePositionRequest(
        Long positionId,
        String positionName,
        Long positionAllowance,
        LocalDate startDate) {
}