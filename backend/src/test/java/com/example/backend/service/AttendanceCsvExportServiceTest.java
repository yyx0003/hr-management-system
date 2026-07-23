package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceCsvExportServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    private AttendanceCsvExportService service;

    @BeforeEach
    void setUp() {

        service = new AttendanceCsvExportService(
                attendanceRepository);
    }

    /**
     * 対象月の勤怠情報がCSV形式で出力されること。
     */
    @Test
    void exportCsvCreatesMonthlyAttendanceCsv() {

        Long employeeId = 10L;
        String employeeNo = "1924";
        YearMonth targetMonth =
                YearMonth.of(2026, 7);

        Attendance normalAttendance =
                createAttendance(
                        employeeId,
                        LocalDate.of(2026, 7, 1),
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        Attendance paidLeaveAttendance =
                createAttendance(
                        employeeId,
                        LocalDate.of(2026, 7, 2),
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31)))
                .thenReturn(
                        List.of(
                                normalAttendance,
                                paidLeaveAttendance));

        CsvFileData result =
                service.exportCsv(
                        employeeId,
                        employeeNo,
                        targetMonth);

        assertEquals(
                "勤怠_1924_2026-07.csv",
                result.getFileName());

        String lineSeparator =
                System.lineSeparator();

        String expectedCsv =
                "\uFEFF"
                        + "employeeNo,workDate,"
                        + "attendanceTime,leavingTime,workType"
                        + lineSeparator
                        + "1924,2026-07-01,"
                        + "09:00,18:00,NORMAL"
                        + lineSeparator
                        + "1924,2026-07-02,"
                        + ",,PAID_LEAVE"
                        + lineSeparator;

        String actualCsv =
                new String(
                        result.getContent(),
                        StandardCharsets.UTF_8);

        assertEquals(
                expectedCsv,
                actualCsv);

        verify(attendanceRepository)
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 7, 31));
    }

    /**
     * 対象月に勤怠情報がない場合、
     * ヘッダーだけのCSVが出力されること。
     */
    @Test
    void exportCsvCreatesHeaderOnlyWhenNoAttendanceExists() {

        Long employeeId = 10L;
        String employeeNo = "1924";
        YearMonth targetMonth =
                YearMonth.of(2026, 8);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of());

        CsvFileData result =
                service.exportCsv(
                        employeeId,
                        employeeNo,
                        targetMonth);

        assertEquals(
                "勤怠_1924_2026-08.csv",
                result.getFileName());

        String expectedCsv =
                "\uFEFF"
                        + "employeeNo,workDate,"
                        + "attendanceTime,leavingTime,workType"
                        + System.lineSeparator();

        String actualCsv =
                new String(
                        result.getContent(),
                        StandardCharsets.UTF_8);

        assertEquals(
                expectedCsv,
                actualCsv);

        verify(attendanceRepository)
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31));
    }

    /**
     * テスト用の勤怠情報を作成する。
     */
    private Attendance createAttendance(
            Long employeeId,
            LocalDate workDate,
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        Attendance attendance =
                new Attendance();

        attendance.setEmployeeId(
                employeeId);

        attendance.setWorkDate(
                workDate);

        attendance.setAttendanceTime(
                attendanceTime);

        attendance.setLeavingTime(
                leavingTime);

        attendance.setWorkType(
                workType);

        return attendance;
    }
}

