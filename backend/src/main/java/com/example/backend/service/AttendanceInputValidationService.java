package com.example.backend.service;

import java.time.LocalTime;

import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Attendance;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠入力内容を確認するサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceInputValidationService {

    private final MessageService messageService;

    /**
     * 勤務区分と出退勤時刻の組み合わせを確認する。
     *
     * @param workType 勤務区分
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     */
    public void validate(
            String workType,
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        validateWorkType(workType);

        if (Attendance.WorkType.NORMAL.equals(workType)
                || Attendance.WorkType.HOLIDAY_WORK.equals(workType)) {

            validateRequiredTimes(
                    attendanceTime,
                    leavingTime);

            validateTimeOrder(
                    attendanceTime,
                    leavingTime);

            return;
        }

        validateTimesAreEmpty(
                attendanceTime,
                leavingTime);
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

        boolean valid =
                Attendance.WorkType.NORMAL.equals(workType)
                        || Attendance.WorkType.HOLIDAY_WORK.equals(workType)
                        || Attendance.WorkType.PAID_LEAVE.equals(workType)
                        || Attendance.WorkType.ABSENCE.equals(workType);

        if (!valid) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workType.invalid"));
        }
    }

    /**
     * 通常勤務・休日出勤時の出退勤時刻を確認する。
     *
     * @param attendanceTime 出勤时刻
     * @param leavingTime 退勤时刻
     */
    private void validateRequiredTimes(
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        if (attendanceTime == null || leavingTime == null) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.required"));
        }
    }

    /**
     * 退勤時刻が出勤時刻より後であることを確認する。
     *
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     */
    private void validateTimeOrder(
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        if (!leavingTime.isAfter(attendanceTime)) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.invalidOrder"));
        }
    }

    /**
     * 有給休暇・欠勤時に出退勤時刻が入力されていないことを確認する。
     *
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     */
    private void validateTimesAreEmpty(
            LocalTime attendanceTime,
            LocalTime leavingTime) {

        if (attendanceTime != null || leavingTime != null) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.notAllowed"));
        }
    }
}