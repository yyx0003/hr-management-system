package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceMonthlyDeleteResponse;
import com.example.backend.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceMonthlyDeleteServiceTest {

    private static final Long LOGIN_EMPLOYEE_ID =
            10L;
    private static final YearMonth TARGET_MONTH =
            YearMonth.of(2026, 7);

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AttendanceDeadlineService deadlineService;

    @Mock
    private MessageService messageService;

    @Mock
    private SalaryResultTransactionHelper salaryResultTransactionHelper;

    private AttendanceMonthlyDeleteService service;

    @BeforeEach
    void setUp() {

        service =
                new AttendanceMonthlyDeleteService(
                        attendanceRepository,
                        deadlineService,
                        messageService,
                        salaryResultTransactionHelper);
    }

    @Test
    void deleteMonthlyAttendancesDeletesOnlyLoginEmployeeTargetMonth() {

        when(attendanceRepository
                .deleteByEmployeeIdAndTargetMonth(
                        LOGIN_EMPLOYEE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)))
                .thenReturn(21);
        when(messageService.getMessage(
                "scr060.delete.success"))
                .thenReturn(
                        "対象月の勤怠データを削除しました。");

        AttendanceMonthlyDeleteResponse response =
                service.deleteMonthlyAttendances(
                        LOGIN_EMPLOYEE_ID,
                        TARGET_MONTH);

        assertEquals(
                21,
                response.deletedCount());
        assertEquals(
                "対象月の勤怠データを削除しました。",
                response.message());

        verify(deadlineService)
                .validateEditable(TARGET_MONTH);
        verify(attendanceRepository)
                .deleteByEmployeeIdAndTargetMonth(
                        LOGIN_EMPLOYEE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));
        verify(salaryResultTransactionHelper)
                .deleteSalaryResultOnly(
                        LOGIN_EMPLOYEE_ID,
                        TARGET_MONTH);
    }

    @Test
    void deleteMonthlyAttendancesReturnsZeroWhenNoDataExists() {

        when(attendanceRepository
                .deleteByEmployeeIdAndTargetMonth(
                        LOGIN_EMPLOYEE_ID,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)))
                .thenReturn(0);
        when(messageService.getMessage(
                "scr060.delete.notfound"))
                .thenReturn(
                        "削除対象の勤怠データが存在しません。");

        AttendanceMonthlyDeleteResponse response =
                service.deleteMonthlyAttendances(
                        LOGIN_EMPLOYEE_ID,
                        TARGET_MONTH);

        assertEquals(
                0,
                response.deletedCount());
        assertEquals(
                "削除対象の勤怠データが存在しません。",
                response.message());
    }

    @Test
    void deleteMonthlyAttendancesDoesNotDeleteAfterDeadline() {

        BusinessException deadlineException =
                new BusinessException(
                        "対象年月の入力期限を過ぎています。");

        org.mockito.Mockito.doThrow(deadlineException)
                .when(deadlineService)
                .validateEditable(TARGET_MONTH);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () ->
                                service.deleteMonthlyAttendances(
                                        LOGIN_EMPLOYEE_ID,
                                        TARGET_MONTH));

        assertEquals(
                deadlineException,
                exception);
        verify(
                attendanceRepository,
                never())
                .deleteByEmployeeIdAndTargetMonth(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        verify(
                salaryResultTransactionHelper,
                never())
                .deleteSalaryResultOnly(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }
}