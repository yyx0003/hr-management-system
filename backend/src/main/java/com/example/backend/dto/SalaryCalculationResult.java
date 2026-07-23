package com.example.backend.dto;

import java.util.List;

/**
 * 給与計算処理（POST /api/salary/calculate）のレスポンス用DTO。
 */
public record SalaryCalculationResult(int processedCount, int skippedCount, List<SkippedEmployee> skippedEmployees) {

    public record SkippedEmployee(Long employeeId, String reason) {
    }
}