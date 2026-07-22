package com.example.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.WorkHoursResult;
import com.example.backend.entity.Attendance;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    /** 1日の基準勤務時間 */
    private static final BigDecimal STANDARD_WORK_HOURS =
            BigDecimal.valueOf(8);

    /** メッセージ取得用 */
    private final MessageService messageService;

    /**
     * 勤務時間と残業時間を計算する。
     *
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     * @param workType 勤務区分
     * @return 勤務時間と残業時間
     */
    public WorkHoursResult calculateWorkHours(
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        validateWorkType(workType);

        // 有給休暇・欠勤は勤務時間なし
        if (Attendance.WorkType.PAID_LEAVE.equals(workType)
                || Attendance.WorkType.ABSENCE.equals(workType)) {

            return new WorkHoursResult(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);
        }

        validateWorkTimes(attendanceTime, leavingTime);

        // 出勤から退勤までの時間を分単位で取得
        long workMinutes =
                Duration.between(attendanceTime, leavingTime).toMinutes();

        // 分を時間に変換
        BigDecimal workHours =
                BigDecimal.valueOf(workMinutes)
                        .divide(
                                BigDecimal.valueOf(60),
                                2,
                                RoundingMode.HALF_UP);

        // 8時間を超えた分を残業時間とする
        BigDecimal overtimeHours =
                workHours.subtract(STANDARD_WORK_HOURS)
                        .max(BigDecimal.ZERO);

        return new WorkHoursResult(
                workHours,
                overtimeHours);
    }

    /**
     * 勤務区分を確認する。
     *
     * @param workType 勤務区分
     */
    private void validateWorkType(String workType) {

        if (workType == null || workType.isBlank()) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workType.required"));
        }

        boolean validWorkType =
                Attendance.WorkType.NORMAL.equals(workType)
                        || Attendance.WorkType.PAID_LEAVE.equals(workType)
                        || Attendance.WorkType.ABSENCE.equals(workType)
                        || Attendance.WorkType.HOLIDAY_WORK.equals(workType);

        if (!validWorkType) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workType.invalid"));
        }
    }

    /**
     * 出退勤時刻を確認する。
     *
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     */
    private void validateWorkTimes(
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        if (attendanceTime == null || leavingTime == null) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.required"));
        }

        if (!leavingTime.isAfter(attendanceTime)) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.invalidOrder"));
        }
    }
}