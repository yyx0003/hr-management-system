package com.example.backend.dto.csv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvFileData {

    private String fileName;

    private byte[] content;
}