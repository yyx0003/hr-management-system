package com.example.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 勤怠CSVの1行分のデータ。
 */
@Getter
@AllArgsConstructor
public class AttendanceCsvRow {

    /**
     * CSV上の行番号。
     */
    private final int lineNumber;

    /**
     * 社員番号。
     */
    private final String employeeNo;

    /**
     * 勤務日。
     */
    private final String workDate;

    /**
     * 出勤時刻。
     */
    private final String attendanceTime;

    /**
     * 退勤時刻。
     */
    private final String leavingTime;

    /**
     * 勤務区分。
     */
    private final String workType;
}