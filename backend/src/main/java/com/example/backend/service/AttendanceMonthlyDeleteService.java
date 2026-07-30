package com.example.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.common.MessageService;
import com.example.backend.dto.attendance.AttendanceMonthlyDeleteResponse;
import com.example.backend.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * ログイン社員の月次勤怠を削除するサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceMonthlyDeleteService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceDeadlineService deadlineService;
    private final MessageService messageService;

    /**
     * 対象社員・対象年月の勤怠情報を全件削除する。
     *
     * @param employeeId ログイン社員ID
     * @param targetMonth 対象年月
     * @return 削除結果
     */
    @Transactional
    public AttendanceMonthlyDeleteResponse deleteMonthlyAttendances(
            Long employeeId,
            YearMonth targetMonth) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "employeeId must not be null");
        }

        if (targetMonth == null) {
            throw new IllegalArgumentException(
                    "targetMonth must not be null");
        }

        deadlineService.validateEditable(targetMonth);

        LocalDate targetMonthStart =
                targetMonth.atDay(1);
        LocalDate targetMonthEnd =
                targetMonth.atEndOfMonth();

        int deletedCount =
                attendanceRepository
                        .deleteByEmployeeIdAndTargetMonth(
                                employeeId,
                                targetMonthStart,
                                targetMonthEnd);

        String messageKey =
                deletedCount > 0
                        ? "scr060.delete.success"
                        : "scr060.delete.notfound";

        return new AttendanceMonthlyDeleteResponse(
                deletedCount,
                messageService.getMessage(messageKey));
    }
}
