package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Attendance;

@ExtendWith(MockitoExtension.class)
class AttendanceInputValidationServiceTest {

    @Mock
    private MessageService messageService;

    private AttendanceInputValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService =
                new AttendanceInputValidationService(messageService);
    }

    @Test
    void normalWithValidTimesIsValid() {

        assertDoesNotThrow(
                () -> validationService.validate(
                        Attendance.WorkType.NORMAL,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0)));
    }

    @Test
    void holidayWorkWithValidTimesIsValid() {

        assertDoesNotThrow(
                () -> validationService.validate(
                        Attendance.WorkType.HOLIDAY_WORK,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0)));
    }

    @Test
    void paidLeaveWithoutTimesIsValid() {

        assertDoesNotThrow(
                () -> validationService.validate(
                        Attendance.WorkType.PAID_LEAVE,
                        null,
                        null));
    }

    @Test
    void absenceWithoutTimesIsValid() {

        assertDoesNotThrow(
                () -> validationService.validate(
                        Attendance.WorkType.ABSENCE,
                        null,
                        null));
    }

    @Test
    void nullWorkTypeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.workType.required"))
                .thenReturn("勤務区分を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                null,
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0)));

        assertEquals(
                "勤務区分を入力してください。",
                exception.getMessage());
    }

    @Test
    void blankWorkTypeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.workType.required"))
                .thenReturn("勤務区分を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                " ",
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0)));

        assertEquals(
                "勤務区分を入力してください。",
                exception.getMessage());
    }

    @Test
    void invalidWorkTypeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.workType.invalid"))
                .thenReturn("勤務区分が正しくありません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                "INVALID",
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0)));

        assertEquals(
                "勤務区分が正しくありません。",
                exception.getMessage());
    }

    @Test
    void normalWithoutAttendanceTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.required"))
                .thenReturn(
                        "出勤時刻および退勤時刻を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                Attendance.WorkType.NORMAL,
                                null,
                                LocalTime.of(18, 0)));

        assertEquals(
                "出勤時刻および退勤時刻を入力してください。",
                exception.getMessage());
    }

    @Test
    void normalWithoutLeavingTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.required"))
                .thenReturn(
                        "出勤時刻および退勤時刻を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                Attendance.WorkType.NORMAL,
                                LocalTime.of(9, 0),
                                null));

        assertEquals(
                "出勤時刻および退勤時刻を入力してください。",
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
                        () -> validationService.validate(
                                Attendance.WorkType.NORMAL,
                                LocalTime.of(18, 0),
                                LocalTime.of(9, 0)));

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
                        () -> validationService.validate(
                                Attendance.WorkType.NORMAL,
                                LocalTime.of(9, 0),
                                LocalTime.of(9, 0)));

        assertEquals(
                "退勤時刻は出勤時刻より後の時刻を入力してください。",
                exception.getMessage());
    }

    @Test
    void paidLeaveWithTimesThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.notAllowed"))
                .thenReturn(
                        "有給休暇・欠勤の場合、出勤時刻および退勤時刻は入力できません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                Attendance.WorkType.PAID_LEAVE,
                                LocalTime.of(9, 0),
                                LocalTime.of(18, 0)));

        assertEquals(
                "有給休暇・欠勤の場合、出勤時刻および退勤時刻は入力できません。",
                exception.getMessage());
    }

    @Test
    void absenceWithAttendanceTimeThrowsException() {

        when(messageService.getMessage(
                "error.attendance.time.notAllowed"))
                .thenReturn(
                        "有給休暇・欠勤の場合、出勤時刻および退勤時刻は入力できません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> validationService.validate(
                                Attendance.WorkType.ABSENCE,
                                LocalTime.of(9, 0),
                                null));

        assertEquals(
                "有給休暇・欠勤の場合、出勤時刻および退勤時刻は入力できません。",
                exception.getMessage());
    }
}