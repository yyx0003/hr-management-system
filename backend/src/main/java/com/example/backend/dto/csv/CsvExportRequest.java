package com.example.backend.dto.csv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CsvExportRequest {

    @NotBlank(message = "{scr080.targetMonth.required}")
    @Pattern(
            regexp = "^$|\\d{4}-(0[1-9]|1[0-2])",
            message = "{scr080.targetMonth.format}")
    private String targetMonth;
}
