package com.example.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvImportError {

    private Integer lineNumber;

    private String itemName;

    private String value;

    private String errorMessage;
}