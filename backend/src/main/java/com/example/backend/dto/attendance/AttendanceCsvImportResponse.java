package com.example.backend.dto.attendance;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AttendanceCsvImportResponse {

    private boolean success;

    private String message;

    private List<CsvImportError> errors;
}