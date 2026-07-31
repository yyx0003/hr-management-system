package com.example.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 給与実績（salary_resultテーブル対応）
 * employee_id + target_year + target_month の複合主キー。
 *
 * total_work_hours・total_overtime_hours・total_holiday_work_hoursはDB側でNUMERIC(6,2)のため、
 * 浮動小数点誤差を避けるためBigDecimalで保持する（double／floatは使用しない）。
 */
@TableName("salary_result")
@Getter
@Setter
@NoArgsConstructor
public class SalaryResult {

    private Long employeeId;
    private Integer targetYear;
    private Integer targetMonth;

    /** 対象年月時点での部署ID（計算時点のスナップショット、外部キーなし） */
    private Long departmentId;

    private BigDecimal totalWorkHours;
    private BigDecimal totalOvertimeHours;

    /** 休日出勤時間（HOLIDAY_WORKの実働時間の合計） */
    private BigDecimal totalHolidayWorkHours;

    private Long totalSalary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}