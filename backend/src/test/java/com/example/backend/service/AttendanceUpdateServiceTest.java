package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceUpdateServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceInputValidationService inputValidationService;

    @Mock
    private AttendanceDeadlineService deadlineService;

    @Mock
    private MessageService messageService;

    @Mock
    private SalaryCalculationService salaryCalculationService;

    @Mock
    private SalaryResultTransactionHelper salaryResultTransactionHelper;

   private AttendanceUpdateService service;

    @BeforeEach
    void setUp() {
        service =
                new AttendanceUpdateService(
                        attendanceRepository,
                        inputValidationService,
                        deadlineService,
                        messageService,
                        salaryCalculationService,
                        salaryResultTransactionHelper);
    }
    

    @Test
    void normalAttendanceIsUpdated() {

        Long employeeId = 1L;
        LocalDate workDate =
                LocalDate.of(2026, 7, 22);

        Attendance existing =
                createAttendance(
                        employeeId,
                        workDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        AttendanceUpdateRequest request =
                createRequest(
                        "08:30",
                        "17:30",
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository.selectOne(
                any(LambdaQueryWrapper.class)))
                .thenReturn(existing);

        Attendance result =
                service.update(
                        employeeId,
                        workDate,
                        request);

        verify(deadlineService)
                .validateEditable(workDate);

        verify(inputValidationService)
                .validate(
                        Attendance.WorkType.NORMAL,
                        LocalTime.of(8, 30),
                        LocalTime.of(17, 30));

        verify(attendanceRepository)
                .update(
                        isNull(),
                        any(UpdateWrapper.class));

        assertEquals(
                LocalTime.of(8, 30),
                result.getAttendanceTime());

        assertEquals(
                LocalTime.of(17, 30),
                result.getLeavingTime());

        assertEquals(
                Attendance.WorkType.NORMAL,
                result.getWorkType());
    }

    @Test
    void normalAttendanceCanBeChangedToPaidLeave() {

        Long employeeId = 1L;
        LocalDate workDate =
                LocalDate.of(2026, 7, 22);

        Attendance existing =
                createAttendance(
                        employeeId,
                        workDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        AttendanceUpdateRequest request =
                createRequest(
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE);

        when(attendanceRepository.selectOne(
                any(LambdaQueryWrapper.class)))
                .thenReturn(existing);

        Attendance result =
                service.update(
                        employeeId,
                        workDate,
                        request);

        verify(inputValidationService)
                .validate(
                        Attendance.WorkType.PAID_LEAVE,
                        null,
                        null);

        assertNull(result.getAttendanceTime());
        assertNull(result.getLeavingTime());

        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                result.getWorkType());
    }

    @Test
    void attendanceNotFoundThrowsException() {

        Long employeeId = 1L;
        LocalDate workDate =
                LocalDate.of(2026, 7, 22);

        AttendanceUpdateRequest request =
                createRequest(
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository.selectOne(
                any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        when(messageService.getMessage(
                eq("error.attendance.notfound"),
                any()))
                .thenReturn(
                        "対象勤務日の勤怠情報が見つかりません。workDate=2026-07-22");

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.update(
                                employeeId,
                                workDate,
                                request));

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatus());

        assertEquals(
                "対象勤務日の勤怠情報が見つかりません。workDate=2026-07-22",
                exception.getMessage());

        verify(attendanceRepository, never())
                .update(
                        isNull(),
                        any(UpdateWrapper.class));
    }

    @Test
    void nullEmployeeIdThrowsException() {

        AttendanceUpdateRequest request =
                createRequest(
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.update(
                                null,
                                LocalDate.of(2026, 7, 22),
                                request));

        assertEquals(
                "employeeId must not be null",
                exception.getMessage());
    }

    @Test
    void nullWorkDateThrowsException() {

        AttendanceUpdateRequest request =
                createRequest(
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.update(
                                1L,
                                null,
                                request));

        assertEquals(
                "workDate must not be null",
                exception.getMessage());
    }

    @Test
    void nullRequestThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.update(
                                1L,
                                LocalDate.of(2026, 7, 22),
                                null));

        assertEquals(
                "request must not be null",
                exception.getMessage());
    }

    @Test
    void invalidAttendanceTimeThrowsException() {

        AttendanceUpdateRequest request =
                createRequest(
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
                        () -> service.update(
                                1L,
                                LocalDate.of(2026, 7, 22),
                                request));

        assertEquals(
                "attendanceTimeの時刻形式が正しくありません。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .selectOne(any(LambdaQueryWrapper.class));

        verify(attendanceRepository, never())
                .update(
                        isNull(),
                        any(UpdateWrapper.class));
    }

    @Test
    void invalidLeavingTimeThrowsException() {

        AttendanceUpdateRequest request =
                createRequest(
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
                        () -> service.update(
                                1L,
                                LocalDate.of(2026, 7, 22),
                                request));

        assertEquals(
                "leavingTimeの時刻形式が正しくありません。",
                exception.getMessage());

        verify(attendanceRepository, never())
                .selectOne(any(LambdaQueryWrapper.class));

        verify(attendanceRepository, never())
                .update(
                        isNull(),
                        any(UpdateWrapper.class));
    }

    @Test
    void updatedEntityIsReturnedWhenExplicitUpdateWrapperIsUsed() {

        Long employeeId = 10L;
        LocalDate workDate =
                LocalDate.of(2026, 7, 22);

        Attendance existing =
                createAttendance(
                        employeeId,
                        workDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        AttendanceUpdateRequest request =
                createRequest(
                        "09:15",
                        "18:30",
                        Attendance.WorkType.HOLIDAY_WORK);

        when(attendanceRepository.selectOne(
                any(LambdaQueryWrapper.class)))
                .thenReturn(existing);

        Attendance updated =
                service.update(
                employeeId,
                workDate,
                request);

        verify(attendanceRepository)
                .update(
                        isNull(),
                        any(UpdateWrapper.class));

        assertEquals(10L, updated.getEmployeeId());

        assertEquals(
                LocalDate.of(2026, 7, 22),
                updated.getWorkDate());

        assertEquals(
                LocalTime.of(9, 15),
                updated.getAttendanceTime());

        assertEquals(
                LocalTime.of(18, 30),
                updated.getLeavingTime());

        assertEquals(
                Attendance.WorkType.HOLIDAY_WORK,
                updated.getWorkType());
    }

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

    private AttendanceUpdateRequest createRequest(
            String attendanceTime,
            String leavingTime,
            String workType) {

        AttendanceUpdateRequest request =
                new AttendanceUpdateRequest();

        request.setAttendanceTime(attendanceTime);
        request.setLeavingTime(leavingTime);
        request.setWorkType(workType);

        return request;
    }
}
