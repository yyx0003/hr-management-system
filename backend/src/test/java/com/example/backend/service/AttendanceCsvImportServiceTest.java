package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.common.MessageService;
import com.example.backend.dto.attendance.AttendanceCsvImportResponse;
import com.example.backend.dto.attendance.AttendanceCsvParseResult;
import com.example.backend.dto.attendance.AttendanceCsvRow;
import com.example.backend.dto.attendance.AttendanceCsvValidatedRow;
import com.example.backend.dto.attendance.AttendanceCsvValidationResult;
import com.example.backend.dto.attendance.CsvImportError;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceCsvImportServiceTest {

    @Mock
    private AttendanceCsvParserService attendanceCsvParserService;

    @Mock
    private AttendanceCsvValidationService attendanceCsvValidationService;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private MultipartFile file;

    private AttendanceCsvImportService service;

    @BeforeEach
    void setUp() {

        service = new AttendanceCsvImportService(
                attendanceCsvParserService,
                attendanceCsvValidationService,
                attendanceRepository,
                messageService);
    }

    @Test
    void validCsvReplacesMonthlyAttendances() {

        Long employeeId = 10L;
        String employeeNo = "1924";

        YearMonth targetMonth = YearMonth.of(2026, 7);

        List<AttendanceCsvRow> parsedRows = List.of(
                createParsedRow(
                        2,
                        employeeNo,
                        "2026-07-01",
                        "09:00",
                        "18:00",
                        Attendance.WorkType.NORMAL),
                createParsedRow(
                        3,
                        employeeNo,
                        "2026-07-02",
                        "",
                        "",
                        Attendance.WorkType.PAID_LEAVE));

        List<AttendanceCsvValidatedRow> validatedRows = List.of(
                createValidatedRow(
                        2,
                        employeeNo,
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL),
                createValidatedRow(
                        3,
                        employeeNo,
                        LocalDate.of(2026, 7, 2),
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE));

        when(attendanceCsvParserService.parse(file))
                .thenReturn(
                        new AttendanceCsvParseResult(
                                parsedRows,
                                List.of()));

        when(attendanceCsvValidationService.validate(
                parsedRows,
                targetMonth,
                employeeNo))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                validatedRows,
                                List.of()));

        when(attendanceRepository.insert(
                any(Attendance.class)))
                .thenReturn(1);

        when(messageService.getMessage(
                eq("scr070.import.success"),
                any(),
                any()))
                .thenReturn(
                        "勤怠CSVを取り込みました。読込件数：2、登録件数：2");

        AttendanceCsvImportResponse response = service.importCsv(
                employeeId,
                employeeNo,
                targetMonth,
                file);

        assertTrue(response.isSuccess());
        assertEquals(
                "勤怠CSVを取り込みました。読込件数：2、登録件数：2",
                response.getMessage());
        assertEquals(0, response.getErrors().size());

        /*
         * 削除後に登録されることを確認する。
         */
        InOrder inOrder = inOrder(attendanceRepository);

        inOrder.verify(attendanceRepository)
                .deleteByEmployeeIdAndTargetMonth(
                        employeeId,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        inOrder.verify(
                attendanceRepository,
                times(2))
                .insert(any(Attendance.class));

        inOrder.verifyNoMoreInteractions();

        verify(attendanceRepository, never())
                .deleteByEmployeeId(any());
    }

    @Test
    void parseErrorDoesNotUpdateDatabase() {

        YearMonth targetMonth = YearMonth.of(2026, 7);

        CsvImportError parseError = new CsvImportError(
                2,
                "CSV",
                "1924,2026-07-01",
                "CSVの列数が正しくありません。");

        AttendanceCsvParseResult parseResult = new AttendanceCsvParseResult(
                List.of(),
                List.of(parseError));

        when(attendanceCsvParserService.parse(file))
                .thenReturn(parseResult);

        when(attendanceCsvValidationService.validate(
                List.of(),
                targetMonth,
                "1924"))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                List.of(),
                                List.of()));

        when(messageService.getMessage(
                eq("scr070.import.hasErrors"),
                any()))
                .thenReturn(
                        "CSV内にエラーが存在するため、登録処理は行われませんでした。エラー件数：1");

        AttendanceCsvImportResponse response = service.importCsv(
                10L,
                "1924",
                targetMonth,
                file);

        assertFalse(response.isSuccess());
        assertEquals(1, response.getErrors().size());
        assertEquals(parseError, response.getErrors().get(0));

        verify(attendanceRepository, never())
                .deleteByEmployeeIdAndTargetMonth(
                        any(),
                        any(),
                        any());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void validationErrorDoesNotUpdateDatabase() {

        YearMonth targetMonth = YearMonth.of(2026, 7);

        AttendanceCsvRow parsedRow = createParsedRow(
                2,
                "9999",
                "2026-07-01",
                "09:00",
                "18:00",
                Attendance.WorkType.NORMAL);

        CsvImportError validationError = new CsvImportError(
                2,
                "社員番号",
                "9999",
                "社員番号が一致しません。");

        List<AttendanceCsvRow> parsedRows = List.of(parsedRow);

        when(attendanceCsvParserService.parse(file))
                .thenReturn(
                        new AttendanceCsvParseResult(
                                parsedRows,
                                List.of()));

        when(attendanceCsvValidationService.validate(
                parsedRows,
                targetMonth,
                "1924"))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                List.of(),
                                List.of(validationError)));

        when(messageService.getMessage(
                eq("scr070.import.hasErrors"),
                any()))
                .thenReturn(
                        "CSV内にエラーが存在するため、登録処理は行われませんでした。エラー件数：1");

        AttendanceCsvImportResponse response = service.importCsv(
                10L,
                "1924",
                targetMonth,
                file);

        assertFalse(response.isSuccess());
        assertEquals(1, response.getErrors().size());
        assertEquals(
                validationError,
                response.getErrors().get(0));

        verify(attendanceRepository, never())
                .deleteByEmployeeIdAndTargetMonth(
                        any(),
                        any(),
                        any());

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void parseAndValidationErrorsAreCombined() {

        YearMonth targetMonth = YearMonth.of(2026, 7);

        AttendanceCsvRow parsedRow = createParsedRow(
                3,
                "9999",
                "2026-07-01",
                "09:00",
                "18:00",
                Attendance.WorkType.NORMAL);

        CsvImportError parseError = new CsvImportError(
                2,
                "CSV",
                "invalid",
                "CSVの列数が正しくありません。");

        CsvImportError validationError = new CsvImportError(
                3,
                "社員番号",
                "9999",
                "社員番号が一致しません。");

        List<AttendanceCsvRow> parsedRows = List.of(parsedRow);

        when(attendanceCsvParserService.parse(file))
                .thenReturn(
                        new AttendanceCsvParseResult(
                                parsedRows,
                                List.of(parseError)));

        when(attendanceCsvValidationService.validate(
                parsedRows,
                targetMonth,
                "1924"))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                List.of(),
                                List.of(validationError)));

        when(messageService.getMessage(
                eq("scr070.import.hasErrors"),
                any()))
                .thenReturn(
                        "エラー件数：2");

        AttendanceCsvImportResponse response = service.importCsv(
                10L,
                "1924",
                targetMonth,
                file);

        assertFalse(response.isSuccess());
        assertEquals(2, response.getErrors().size());
        assertEquals(parseError, response.getErrors().get(0));
        assertEquals(validationError, response.getErrors().get(1));

        verify(attendanceRepository, never())
                .deleteByEmployeeIdAndTargetMonth(
                        any(),
                        any(),
                        any());
    }

    @Test
    void emptyCsvClearsTargetMonth() {

        YearMonth targetMonth = YearMonth.of(2026, 7);

        when(attendanceCsvParserService.parse(file))
                .thenReturn(
                        new AttendanceCsvParseResult(
                                List.of(),
                                List.of()));

        when(attendanceCsvValidationService.validate(
                List.of(),
                targetMonth,
                "1924"))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                List.of(),
                                List.of()));

        when(messageService.getMessage(
                eq("scr070.import.success"),
                any(),
                any()))
                .thenReturn(
                        "勤怠CSVを取り込みました。読込件数：0、登録件数：0");

        AttendanceCsvImportResponse response = service.importCsv(
                10L,
                "1924",
                targetMonth,
                file);

        assertTrue(response.isSuccess());
        assertEquals(0, response.getErrors().size());

        verify(attendanceRepository)
                .deleteByEmployeeIdAndTargetMonth(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));

        verify(attendanceRepository, never())
                .insert(any(Attendance.class));
    }

    @Test
    void insertExceptionIsPropagated() {

        YearMonth targetMonth = YearMonth.of(2026, 7);

        AttendanceCsvRow parsedRow = createParsedRow(
                2,
                "1924",
                "2026-07-01",
                "09:00",
                "18:00",
                Attendance.WorkType.NORMAL);

        AttendanceCsvValidatedRow validatedRow = createValidatedRow(
                2,
                "1924",
                LocalDate.of(2026, 7, 1),
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                Attendance.WorkType.NORMAL);

        List<AttendanceCsvRow> parsedRows = List.of(parsedRow);

        when(attendanceCsvParserService.parse(file))
                .thenReturn(
                        new AttendanceCsvParseResult(
                                parsedRows,
                                List.of()));

        when(attendanceCsvValidationService.validate(
                parsedRows,
                targetMonth,
                "1924"))
                .thenReturn(
                        new AttendanceCsvValidationResult(
                                List.of(validatedRow),
                                List.of()));

        RuntimeException databaseException = new RuntimeException(
                "database insert failed");

        when(attendanceRepository.insert(
                any(Attendance.class)))
                .thenThrow(databaseException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.importCsv(
                        10L,
                        "1924",
                        targetMonth,
                        file));

        assertEquals(
                "database insert failed",
                exception.getMessage());

        verify(attendanceRepository)
                .deleteByEmployeeIdAndTargetMonth(
                        10L,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));
    }

    @Test
    void nullEmployeeIdThrowsException() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importCsv(
                        null,
                        "1924",
                        YearMonth.of(2026, 7),
                        file));

        assertEquals(
                "employeeId must not be null",
                exception.getMessage());

        verify(attendanceCsvParserService, never())
                .parse(any());
    }

    @Test
    void blankEmployeeNoThrowsException() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importCsv(
                        10L,
                        " ",
                        YearMonth.of(2026, 7),
                        file));

        assertEquals(
                "employeeNo must not be blank",
                exception.getMessage());

        verify(attendanceCsvParserService, never())
                .parse(any());
    }

    @Test
    void nullTargetMonthThrowsException() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.importCsv(
                        10L,
                        "1924",
                        null,
                        file));

        assertEquals(
                "targetMonth must not be null",
                exception.getMessage());

        verify(attendanceCsvParserService, never())
                .parse(any());
    }

    private AttendanceCsvRow createParsedRow(
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

    private AttendanceCsvValidatedRow createValidatedRow(
            int lineNumber,
            String employeeNo,
            LocalDate workDate,
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        return new AttendanceCsvValidatedRow(
                lineNumber,
                employeeNo,
                workDate,
                attendanceTime,
                leavingTime,
                workType);
    }
}