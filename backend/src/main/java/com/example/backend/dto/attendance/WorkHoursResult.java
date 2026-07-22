package com.example.backend.dto.attendance;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorkHoursResult {

    private BigDecimal workHours;

    private BigDecimal overtimeHours;
}