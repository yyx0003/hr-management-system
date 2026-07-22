package com.example.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{scr010.employeeNo.required}") String employeeNo,
        @NotBlank(message = "{scr010.password.required}") String password
) {
}
