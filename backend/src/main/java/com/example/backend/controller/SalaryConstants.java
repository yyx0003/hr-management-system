package com.example.backend.controller;

import java.math.BigDecimal;

public final class SalaryConstants {
    // 标准工时：1天8小时
    public static final BigDecimal STANDARD_WORK_HOURS = new BigDecimal("8.00");

    // 高度资格：第2个起，每个加算10000円
    public static final long ADVANCE_QUALIFICATION_ALLOWANCE = 10_000L;

    // 所属年给：初年度5000円
    public static final long SENIORITY_PAY_BASE = 5_000L;

    // 所属年给：每年递增5000円
    public static final long SENIORITY_PAY_INCREMENT = 5_000L;

    // 退职者数据保留3个月才删除
    public static final int RETENTION_MONTHS = 3;

    // 考勤修改截止日：次月5号
    public static final int ATTENDANCE_EDIT_DEADLINE_DAY = 5;

    // 時給換算の基準月間時間：160時間
    public static final BigDecimal STANDARD_MONTHLY_HOURS = BigDecimal.valueOf(160);

    // 残業時間の割増率：1.25倍
    public static final BigDecimal OVERTIME_RATE = new BigDecimal("1.25");

    // 休日出勤時間の割増率：1.5倍
    public static final BigDecimal HOLIDAY_WORK_RATE = new BigDecimal("1.5");
}