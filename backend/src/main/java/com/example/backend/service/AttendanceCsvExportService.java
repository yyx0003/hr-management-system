package com.example.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.backend.dto.csv.CsvFileData;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠CSVを出力するサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceCsvExportService {

    private static final String CSV_HEADER =
            "employeeNo,workDate,attendanceTime,leavingTime,workType";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final String UTF_8_BOM = "\uFEFF";

    private final AttendanceRepository attendanceRepository;

    /**
     * ログイン社員の月次勤怠CSVを作成する。
     *
     * @param employeeId 社員ID
     * @param employeeNo 社員番号
     * @param targetMonth 対象年月
     * @return CSVファイル情報
     */
    public CsvFileData exportCsv(
            Long employeeId,
            String employeeNo,
            YearMonth targetMonth) {

        List<Attendance> attendances =
                attendanceRepository
                        .findByEmployeeIdAndTargetMonth(
                                employeeId,
                                targetMonth.atDay(1),
                                targetMonth.atEndOfMonth());

        StringBuilder csv =
                new StringBuilder();

        csv.append(UTF_8_BOM);
        csv.append(CSV_HEADER);
        csv.append(System.lineSeparator());

        for (Attendance attendance : attendances) {
            appendCsvRow(
                    csv,
                    employeeNo,
                    attendance);
        }

        String fileName =
                String.format(
                        "勤怠_%s_%s.csv",
                        employeeNo,
                        targetMonth);

        return new CsvFileData(
                fileName,
                csv.toString()
                        .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 勤怠情報をCSVの1行として追加する。
     *
     * @param csv CSV文字列
     * @param employeeNo 社員番号
     * @param attendance 勤怠情報
     */
    private void appendCsvRow(
            StringBuilder csv,
            String employeeNo,
            Attendance attendance) {

        csv.append(employeeNo);
        csv.append(",");
        csv.append(attendance.getWorkDate());
        csv.append(",");
        csv.append(formatTime(
                attendance.getAttendanceTime()));
        csv.append(",");
        csv.append(formatTime(
                attendance.getLeavingTime()));
        csv.append(",");
        csv.append(attendance.getWorkType());
        csv.append(System.lineSeparator());
    }

    /**
     * 時刻をHH:mm形式へ変換する。
     * 時刻がない場合は空文字を返す。
     *
     * @param time 時刻
     * @return CSV出力用の時刻
     */
    private String formatTime(
            LocalTime time) {

        if (time == null) {
            return "";
        }

        return time.format(TIME_FORMATTER);
    }
}