package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCreateRequest;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠情報の新規登録を行うサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceRegistrationService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceInputValidationService inputValidationService;
    private final AttendanceDeadlineService deadlineService;
    private final MessageService messageService;

    /**
     * 勤怠情報を新規登録する。
     *
     * 給与計算およびsalary_resultの更新は行わない。
     *
     * @param employeeId 社員ID
     * @param request 登録内容
     * @return 登録した勤怠情報
     */
    @Transactional
    public Attendance register(
            Long employeeId,
            AttendanceCreateRequest request) {

        validateArguments(employeeId, request);

        LocalDate workDate =
                parseWorkDate(request.getWorkDate());

        LocalTime attendanceTime =
                parseTime(
                        request.getAttendanceTime(),
                        "attendanceTime");

        LocalTime leavingTime =
                parseTime(
                        request.getLeavingTime(),
                        "leavingTime");

        deadlineService.validateEditable(workDate);

        inputValidationService.validate(
                request.getWorkType(),
                attendanceTime,
                leavingTime);

        validateNotRegistered(
                employeeId,
                workDate);

        Attendance attendance =
                createAttendance(
                        employeeId,
                        workDate,
                        attendanceTime,
                        leavingTime,
                        request.getWorkType());

        attendanceRepository.insert(attendance);

        return attendance;
    }

    /**
     * 登録引数を確認する。
     *
     * @param employeeId 社員ID
     * @param request 登録内容
     */
    private void validateArguments(
            Long employeeId,
            AttendanceCreateRequest request) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "employeeId must not be null");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "request must not be null");
        }
    }

    /**
     * 勤務日をLocalDateへ変換する。
     *
     * @param workDate 勤務日文字列
     * @return 勤務日
     */
    private LocalDate parseWorkDate(String workDate) {

        if (workDate == null || workDate.isBlank()) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workDate.required"));
        }

        try {
            return LocalDate.parse(workDate);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.workDate.invalid"));
        }
    }

    /**
     * 時刻文字列をLocalTimeへ変換する。
     *
     * 空文字の場合はnullを返す。
     *
     * @param time 時刻文字列
     * @param fieldName 項目名
     * @return 時刻
     */
    private LocalTime parseTime(
            String time,
            String fieldName) {

        if (time == null || time.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(time);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.time.invalidFormat",
                            fieldName));
        }
    }

    /**
     * 同一社員・同一勤務日の勤怠が未登録であることを確認する。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     */
    private void validateNotRegistered(
            Long employeeId,
            LocalDate workDate) {

        LambdaQueryWrapper<Attendance> query =
                new LambdaQueryWrapper<>();

        query.eq(
                Attendance::getEmployeeId,
                employeeId);

        query.eq(
                Attendance::getWorkDate,
                workDate);

        Long count =
                attendanceRepository.selectCount(query);

        if (count != null && count > 0) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.duplicate",
                            workDate));
        }
    }

    /**
     * 登録用の勤怠Entityを生成する。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     * @param attendanceTime 出勤時刻
     * @param leavingTime 退勤時刻
     * @param workType 勤務区分
     * @return 勤怠Entity
     */
    private Attendance createAttendance(
            Long employeeId,
            LocalDate workDate,
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        Attendance attendance =
                new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(attendanceTime);
        attendance.setLeavingTime(leavingTime);
        attendance.setWorkType(workType);

        return attendance;
    }
}