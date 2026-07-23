package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCreateRequest;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceRegistrationServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceInputValidationService inputValidationService;

    @Mock
    private AttendanceDeadlineService deadlineService;

    @Mock
    private MessageService messageService;

    private AttendanceRegistrationService service;

    @BeforeEach
    void setUp() {
        service =
                new AttendanceRegistrationService(
                        attendanceRepository,
                        inputValidationService,
                        deadlineService,
                        messageService);
    }

    @Test
    void normalAttendanceIsRegistered() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        Attendance result =
                service.register(
                        1L,
                        request);

        verify(deadlineService)
                .validateEditable(
                        LocalDate.of(2026, 7, 22));

        verify(inputValidationService)
                .validate(
                        Attendance.WorkType.NORMAL,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0));

        verify(attendanceRepository)
                .insert(any(Attendance.class));

        assertEquals(1L, result.getEmployeeId());
        assertEquals(
                LocalDate.of(2026, 7, 22),
                result.getWorkDate());
        assertEquals(
                LocalTime.of(9, 0),
                result.getAttendanceTime());
        assertEquals(
                LocalTime.of(18, 0),
                result.getLeavingTime());
        assertEquals(
                Attendance.WorkType.NORMAL,
                result.getWorkType());
    }

    @Test
    void paidLeaveIsRegisteredWithoutTimes() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-24",
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE);

        when(attendanceRepository.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        Attendance result =
                service.register(
                        1L,
                        request);

        verify(inputValidationService)
                .validate(
                        Attendance.WorkType.PAID_LEAVE,
                        null,
                        null);

        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                result.getWorkType());

        assertEquals(null, result.getAttendanceTime());
        assertEquals(null, result.getLeavingTime());
    }

    @Test
    void duplicateAttendanceThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(1L);

        when(messageService.getMessage(
                eq("error.attendance.duplicate"),
                any()))
                .thenReturn(
                        "同一勤務日の勤怠情報は既に登録されています。workDate=2026-07-22");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.register(
                                1L,
                                request));

        assertEquals(
                "同一勤務日の勤怠情報は既に登録されています。workDate=2026-07-22",
                exception.getMessage());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void nullEmployeeIdThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.register(
                                null,
                                request));

        assertEquals(
                "employeeId must not be null",
                exception.getMessage());
    }

    @Test
    void nullRequestThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.register(
                                1L,
                                null));

        assertEquals(
                "request must not be null",
                exception.getMessage());
    }

    @Test
    void blankWorkDateThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        " ",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(messageService.getMessage(
                "error.attendance.workDate.required"))
                .thenReturn("勤務日を入力してください。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.register(
                                1L,
                                request));

        assertEquals(
                "勤務日を入力してください。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void invalidWorkDateThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026/07/22",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(messageService.getMessage(
                "error.attendance.workDate.invalid"))
                .thenReturn(
                        "勤務日の形式が正しくありません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.register(
                                1L,
                                request));

        assertEquals(
                "勤務日の形式が正しくありません。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void invalidAttendanceTimeThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "9時",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(messageService.getMessage(
                eq("error.attendance.time.invalidFormat"),
                any()))
                .thenReturn(
                        "attendanceTimeの時刻形式が正しくありません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.register(
                                1L,
                                request));

        assertEquals(
                "attendanceTimeの時刻形式が正しくありません。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void invalidLeavingTimeThrowsException() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "09:00",
                        "18時",
                        Attendance.WorkType.NORMAL);

        when(messageService.getMessage(
                eq("error.attendance.time.invalidFormat"),
                any()))
                .thenReturn(
                        "leavingTimeの時刻形式が正しくありません。");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.register(
                                1L,
                                request));

        assertEquals(
                "leavingTimeの時刻形式が正しくありません。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void registeredEntityIsPassedToRepository() {

        AttendanceCreateRequest request =
                createRequest(
                        "2026-07-22",
                        "09:15",
                        "18:30",
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository.selectCount(
                any(LambdaQueryWrapper.class)))
                .thenReturn(0L);

        service.register(
                10L,
                request);

        ArgumentCaptor<Attendance> captor =
                ArgumentCaptor.forClass(
                        Attendance.class);

        verify(attendanceRepository)
                .insert(captor.capture());

        Attendance saved =
                captor.getValue();

        assertEquals(10L, saved.getEmployeeId());
        assertEquals(
                LocalDate.of(2026, 7, 22),
                saved.getWorkDate());
        assertEquals(
                LocalTime.of(9, 15),
                saved.getAttendanceTime());
        assertEquals(
                LocalTime.of(18, 30),
                saved.getLeavingTime());
        assertEquals(
                Attendance.WorkType.NORMAL,
                saved.getWorkType());
    }

    private AttendanceCreateRequest createRequest(
            String workDate,
            String attendanceTime,
            String leavingTime,
            String workType) {

        AttendanceCreateRequest request =
                new AttendanceCreateRequest();

        request.setWorkDate(workDate);
        request.setAttendanceTime(attendanceTime);
        request.setLeavingTime(leavingTime);
        request.setWorkType(workType);

        return request;
    }
}