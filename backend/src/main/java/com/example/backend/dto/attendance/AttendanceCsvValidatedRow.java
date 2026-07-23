package com.example.backend.dto.attendance;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 校验完成后的勤怠CSV数据。
 */
@Getter
@AllArgsConstructor
public class AttendanceCsvValidatedRow {

    /**
     * CSV上的行号。
     */
    private final int lineNumber;

    /**
     * 社员编号。
     */
    private final String employeeNo;

    /**
     * 工作日期。
     */
    private final LocalDate workDate;

    /**
     * 上班时间。
     */
    private final LocalTime attendanceTime;

    /**
     * 下班时间。
     */
    private final LocalTime leavingTime;

    /**
     * 勤务区分。
     */
    private final String workType;
}