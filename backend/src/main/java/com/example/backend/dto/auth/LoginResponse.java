package com.example.backend.dto.auth;

public record LoginResponse(
        String token,
        Long employeeId,
        String employeeNo,
        String employeeName
) {
}
