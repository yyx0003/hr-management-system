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

    /** 小数点以下の桁数 */
    private static final int SCALE = 2;

    /** 丸め方法 */
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** 1日の基準勤務時間 */
    private static final BigDecimal STANDARD_WORK_HOURS =
            BigDecimal.valueOf(8).setScale(SCALE, ROUNDING);

    /** 0時間 */
    private static final BigDecimal ZERO_HOURS =
            BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    /** メッセージ取得用 */
    private final MessageService messageService;

    public WorkHoursResult calculateWorkHours(
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        validateWorkType(workType);

        if (Attendance.WorkType.PAID_LEAVE.equals(workType)) {
            return new WorkHoursResult(
                    STANDARD_WORK_HOURS,
                    ZERO_HOURS,
                    ZERO_HOURS,
                    ZERO_HOURS);
        }

        if (Attendance.WorkType.ABSENCE.equals(workType)) {
            return WorkHoursResult.zero();
        }

        validateWorkTimes(attendanceTime, leavingTime);

        BigDecimal actualHours =
                calculateActualHours(attendanceTime, leavingTime);

        if (Attendance.WorkType.HOLIDAY_WORK.equals(workType)) {
            return new WorkHoursResult(
                    ZERO_HOURS,
                    ZERO_HOURS,
                    actualHours,
                    ZERO_HOURS);
        }

        BigDecimal overtimeHours =
                actualHours.subtract(STANDARD_WORK_HOURS)
                        .max(ZERO_HOURS)
                        .setScale(SCALE, ROUNDING);

        BigDecimal regularWorkHours =
                actualHours.subtract(overtimeHours)
                        .setScale(SCALE, ROUNDING);

        BigDecimal shortfallHours =
                STANDARD_WORK_HOURS.subtract(actualHours)
                        .max(ZERO_HOURS)
                        .setScale(SCALE, ROUNDING);

        return new WorkHoursResult(
                regularWorkHours,
                overtimeHours,
                ZERO_HOURS,
                shortfallHours);
    }

    private BigDecimal calculateActualHours(
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        long workMinutes =
                Duration.between(attendanceTime, leavingTime)
                        .toMinutes();

        return BigDecimal.valueOf(workMinutes)
                .divide(
                        BigDecimal.valueOf(60),
                        SCALE,
                        ROUNDING);
    }

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