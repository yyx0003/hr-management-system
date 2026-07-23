package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠情報の更新を行うサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceUpdateService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceInputValidationService inputValidationService;
    private final AttendanceDeadlineService deadlineService;
    private final MessageService messageService;

    /**
     * 対象社員・対象勤務日の勤怠情報を更新する。
     *
     * 給与計算およびsalary_resultの更新は行わない。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     * @param request 更新内容
     * @return 更新後の勤怠情報
     */
    @Transactional
    public Attendance update(
            Long employeeId,
            LocalDate workDate,
            AttendanceUpdateRequest request) {

        validateArguments(
                employeeId,
                workDate,
                request);

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

        Attendance attendance =
                findAttendance(
                        employeeId,
                        workDate);

        attendance.setAttendanceTime(attendanceTime);
        attendance.setLeavingTime(leavingTime);
        attendance.setWorkType(request.getWorkType());

        LambdaQueryWrapper<Attendance> updateCondition =
                createCondition(
                        employeeId,
                        workDate);

        attendanceRepository.update(
                attendance,
                updateCondition);

        return attendance;
    }

    /**
     * 更新引数を確認する。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     * @param request 更新内容
     */
    private void validateArguments(
            Long employeeId,
            LocalDate workDate,
            AttendanceUpdateRequest request) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "employeeId must not be null");
        }

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "workDate must not be null");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "request must not be null");
        }
    }

    /**
     * 時刻文字列をLocalTimeへ変換する。
     *
     * nullまたは空文字の場合はnullを返す。
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
     * 対象の勤怠情報を取得する。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     * @return 勤怠情報
     */
    private Attendance findAttendance(
            Long employeeId,
            LocalDate workDate) {

        Attendance attendance =
                attendanceRepository.selectOne(
                        createCondition(
                                employeeId,
                                workDate));

        if (attendance == null) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    messageService.getMessage(
                            "error.attendance.notfound",
                            workDate));
        }

        return attendance;
    }

    /**
     * 社員ID・勤務日の検索条件を生成する。
     *
     * @param employeeId 社員ID
     * @param workDate 勤務日
     * @return 検索条件
     */
    private LambdaQueryWrapper<Attendance> createCondition(
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

        return query;
    }
}