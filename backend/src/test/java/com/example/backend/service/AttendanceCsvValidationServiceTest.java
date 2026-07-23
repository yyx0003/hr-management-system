package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCsvRow;
import com.example.backend.dto.attendance.AttendanceCsvValidatedRow;
import com.example.backend.dto.attendance.AttendanceCsvValidationResult;
import com.example.backend.dto.attendance.CsvImportError;
import com.example.backend.entity.Attendance;

@ExtendWith(MockitoExtension.class)
class AttendanceCsvValidationServiceTest {

    @Mock
    private AttendanceDeadlineService
            attendanceDeadlineService;

    @Mock
    private AttendanceInputValidationService
            attendanceInputValidationService;

    @Mock
    private MessageService messageService;

    private AttendanceCsvValidationService service;

    @BeforeEach
    void setUp() {

        service =
                new AttendanceCsvValidationService(
                        attendanceDeadlineService,
                        attendanceInputValidationService,
                        messageService);
    }

    @Test
    void validRowsAreConverted() {

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-07-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL),
                        createRow(
                                3,
                                "1924",
                                "2026-07-02",
                                "",
                                "",
                                Attendance.WorkType.PAID_LEAVE));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrors().size());
        assertEquals(2, result.getRows().size());

        AttendanceCsvValidatedRow firstRow =
                result.getRows().get(0);

        assertEquals(2, firstRow.getLineNumber());
        assertEquals("1924", firstRow.getEmployeeNo());
        assertEquals(
                LocalDate.of(2026, 7, 1),
                firstRow.getWorkDate());
        assertEquals(
                LocalTime.of(9, 0),
                firstRow.getAttendanceTime());
        assertEquals(
                LocalTime.of(18, 0),
                firstRow.getLeavingTime());
        assertEquals(
                Attendance.WorkType.NORMAL,
                firstRow.getWorkType());

        AttendanceCsvValidatedRow secondRow =
                result.getRows().get(1);

        assertEquals(
                LocalDate.of(2026, 7, 2),
                secondRow.getWorkDate());
        assertEquals(
                null,
                secondRow.getAttendanceTime());
        assertEquals(
                null,
                secondRow.getLeavingTime());
        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                secondRow.getWorkType());

        verify(attendanceDeadlineService)
                .validateEditable(
                        YearMonth.of(2026, 7));

        verify(attendanceInputValidationService)
                .validate(
                        Attendance.WorkType.NORMAL,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0));

        verify(attendanceInputValidationService)
                .validate(
                        Attendance.WorkType.PAID_LEAVE,
                        null,
                        null);
    }

    @Test
    void employeeNumberMismatchIsReturnedAsError() {

        when(messageService.getMessage(
                eq("scr070.employeeNo.mismatch"),
                any()))
                .thenReturn(
                        "CSV内の社員番号がログイン中の社員番号と一致しません。line=2");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "9999",
                                "2026-07-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        CsvImportError error =
                result.getErrors().get(0);

        assertEquals(2, error.getLineNumber());
        assertEquals("社員番号", error.getItemName());
        assertEquals("9999", error.getValue());
        assertEquals(
                "CSV内の社員番号がログイン中の社員番号と一致しません。line=2",
                error.getErrorMessage());
    }

    @Test
    void invalidWorkDateIsReturnedAsError() {

        when(messageService.getMessage(
                "scr070.workDate.invalid"))
                .thenReturn(
                        "勤務日の形式が正しくありません。yyyy-MM-dd形式で入力してください。");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026/07/01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        CsvImportError error =
                result.getErrors().get(0);

        assertEquals(2, error.getLineNumber());
        assertEquals("勤務日", error.getItemName());
        assertEquals("2026/07/01", error.getValue());
    }

    @Test
    void differentTargetMonthIsReturnedAsError() {

        when(messageService.getMessage(
                eq("scr070.workDate.mismatch"),
                any()))
                .thenReturn(
                        "CSV内の勤務日が指定した対象年月と一致しません。line=2");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-08-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        assertEquals(
                "勤務日",
                result.getErrors()
                        .get(0)
                        .getItemName());
    }

    @Test
    void duplicateWorkDateIsReturnedAsError() {

        when(messageService.getMessage(
                eq("scr070.workDate.duplicate"),
                any()))
                .thenReturn(
                        "同一勤務日のデータがCSV内に複数存在します。workDate=2026-07-01");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-07-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL),
                        createRow(
                                3,
                                "1924",
                                "2026-07-01",
                                "10:00",
                                "19:00",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());

        /*
         * 最初の行は正常なので保持される。
         * 重複した2行目だけがエラーになる。
         */
        assertEquals(1, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        CsvImportError error =
                result.getErrors().get(0);

        assertEquals(3, error.getLineNumber());
        assertEquals("勤務日", error.getItemName());
        assertEquals("2026-07-01", error.getValue());
    }

    @Test
    void invalidTimeFormatIsReturnedAsError() {

        when(messageService.getMessage(
                "scr070.time.invalid"))
                .thenReturn(
                        "時刻の形式が正しくありません。HH:mm形式で入力してください。");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-07-01",
                                "abc",
                                "18:00",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        CsvImportError error =
                result.getErrors().get(0);

        assertEquals("出勤時刻", error.getItemName());
        assertEquals("abc", error.getValue());

        /*
         * 時刻形式が不正な場合は、
         * 勤務区分と時刻の組み合わせ確認を行わない。
         */
        verify(attendanceInputValidationService, never())
                .validate(
                        any(),
                        any(),
                        any());
    }

    @Test
    void businessValidationErrorIsAddedToErrorList() {

        doThrow(
                new BusinessException(
                        "出勤時刻と退勤時刻を入力してください。"))
                .when(attendanceInputValidationService)
                .validate(
                        Attendance.WorkType.NORMAL,
                        null,
                        null);

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-07-01",
                                "",
                                "",
                                Attendance.WorkType.NORMAL));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(1, result.getErrors().size());

        CsvImportError error =
                result.getErrors().get(0);

        assertEquals(
                "勤務区分・出退勤時刻",
                error.getItemName());

        assertEquals(
                "出勤時刻と退勤時刻を入力してください。",
                error.getErrorMessage());
    }

    @Test
    void multipleRowErrorsAreCollected() {

        when(messageService.getMessage(
                eq("scr070.employeeNo.mismatch"),
                any()))
                .thenReturn(
                        "社員番号が一致しません。");

        when(messageService.getMessage(
                "scr070.workDate.invalid"))
                .thenReturn(
                        "勤務日の形式が正しくありません。");

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "9999",
                                "2026-07-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL),
                        createRow(
                                3,
                                "1924",
                                "invalid-date",
                                "",
                                "",
                                Attendance.WorkType.PAID_LEAVE));

        AttendanceCsvValidationResult result =
                service.validate(
                        rows,
                        YearMonth.of(2026, 7),
                        "1924");

        /*
         * 1行目：社員番号エラー
         * 2行目：勤務日形式エラー
         */
        assertTrue(result.hasErrors());
        assertEquals(0, result.getRows().size());
        assertEquals(2, result.getErrors().size());

        assertEquals(
                2,
                result.getErrors()
                        .get(0)
                        .getLineNumber());

        assertEquals(
                3,
                result.getErrors()
                        .get(1)
                        .getLineNumber());
    }

    @Test
    void deadlineErrorIsThrownBeforeRowValidation() {

        YearMonth targetMonth =
                YearMonth.of(2026, 7);

        doThrow(
                new BusinessException(
                        "対象年月の入力期限を過ぎています。"))
                .when(attendanceDeadlineService)
                .validateEditable(targetMonth);

        List<AttendanceCsvRow> rows =
                List.of(
                        createRow(
                                2,
                                "1924",
                                "2026-07-01",
                                "09:00",
                                "18:00",
                                Attendance.WorkType.NORMAL));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> service.validate(
                                rows,
                                targetMonth,
                                "1924"));

        assertEquals(
                "対象年月の入力期限を過ぎています。",
                exception.getMessage());

        verify(attendanceInputValidationService, never())
                .validate(
                        any(),
                        any(),
                        any());
    }

    @Test
    void nullTargetMonthThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.validate(
                                List.of(),
                                null,
                                "1924"));

        assertEquals(
                "targetMonth must not be null",
                exception.getMessage());
    }

    @Test
    void blankLoginEmployeeNoThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.validate(
                                List.of(),
                                YearMonth.of(2026, 7),
                                " "));

        assertEquals(
                "loginEmployeeNo must not be blank",
                exception.getMessage());

        verify(attendanceDeadlineService, never())
                .validateEditable(
                        any(YearMonth.class));
    }

    /**
     * テスト用のCSV行を作成する。
     */
    private AttendanceCsvRow createRow(
            int lineNumber,
            String employeeNo,
            String workDate,
            String attendanceTime,
            String leavingTime,
            String workType) {

        return new AttendanceCsvRow(
                lineNumber,
                employeeNo,
                workDate,
                attendanceTime,
                leavingTime,
                workType);
    }
}