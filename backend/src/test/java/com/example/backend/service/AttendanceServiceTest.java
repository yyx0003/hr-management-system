package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.WorkHoursResult;
import com.example.backend.entity.Attendance;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private MessageService messageService;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(messageService);
    }

    @Test
    void normalWorkEightHours() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        Attendance.WorkType.NORMAL);

        assertEquals(
                0,
                new BigDecimal("8.00")
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void normalWorkWithOvertime() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        assertEquals(
                0,
                new BigDecimal("9.00")
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                new BigDecimal("1.00")
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void normalWorkWithThirtyMinutesOvertime() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        LocalTime.of(9, 30),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        assertEquals(
                0,
                new BigDecimal("8.50")
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                new BigDecimal("0.50")
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void paidLeaveReturnsZero() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE);

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void absenceReturnsZero() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        null,
                        null,
                        Attendance.WorkType.ABSENCE);

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                BigDecimal.ZERO
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void holidayWorkCanBeCalculated() {

        WorkHoursResult result =
                attendanceService.calculateWorkHours(
                        LocalTime.of(10, 0),
                        LocalTime.of(19, 0),
                        Attendance.WorkType.HOLIDAY_WORK);

        assertEquals(
                0,
                new BigDecimal("9.00")
                        .compareTo(result.getWorkHours()));

        assertEquals(
                0,
                new BigDecimal("1.00")
                        .compareTo(result.getOvertimeHours()));
    }

    @Test
    void missingAttendanceTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.required"))
                .thenReturn(
                        "通常勤務・休日出勤の場合、出勤時刻および退勤時刻を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> attendanceService.calculateWorkHours(
                                null,
                                LocalTime.of(18, 0),
                                Attendance.WorkType.NORMAL));

        assertEquals(
                "通常勤務・休日出勤の場合、出勤時刻および退勤時刻を入力してください。",
                exception.getMessage());
    }

    @Test
    void leavingTimeBeforeAttendanceTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.invalidOrder"))
                .thenReturn(
                        "退勤時刻は出勤時刻より後の時刻を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> attendanceService.calculateWorkHours(
                                LocalTime.of(18, 0),
                                LocalTime.of(9, 0),
                                Attendance.WorkType.NORMAL));

        assertEquals(
                "退勤時刻は出勤時刻より後の時刻を入力してください。",
                exception.getMessage());
    }

    @Test
    void sameAttendanceAndLeavingTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.invalidOrder"))
                .thenReturn(
                        "退勤時刻は出勤時刻より後の時刻を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> attendanceService.calculateWorkHours(
                                LocalTime.of(9, 0),
                                LocalTime.of(9, 0),
                                Attendance.WorkType.NORMAL));

        assertEquals(
                "退勤時刻は出勤時刻より後の時刻を入力してください。",
                exception.getMessage());
    }

    @Test
    void workTypeRequiredThrowsException() {

        when(messageService.getMessage(
                "error.attendance.workType.required"))
                .thenReturn(
                        "勤務区分を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> attendanceService.calculateWorkHours(
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0),
                                null));

        assertEquals(
                "勤務区分を入力してください。",
                exception.getMessage());
    }

    @Test
    void invalidWorkTypeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.workType.invalid"))
                .thenReturn(
                        "勤務区分が正しくありません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> attendanceService.calculateWorkHours(
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0),
                                "INVALID"));

        assertEquals(
                "勤務区分が正しくありません。",
                exception.getMessage());
    }
}