package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.attendance.AttendanceCsvRow;
import com.example.backend.dto.attendance.AttendanceCsvValidatedRow;
import com.example.backend.dto.attendance.AttendanceCsvValidationResult;
import com.example.backend.dto.attendance.CsvImportError;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠CSVの入力内容を確認するサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceCsvValidationService {

    /**
     * 日付形式。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd")
            .withResolverStyle(
                    ResolverStyle.STRICT);

    /**
     * 时刻形式。
     */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("HH:mm")
            .withResolverStyle(
                    ResolverStyle.STRICT);

    private final AttendanceDeadlineService attendanceDeadlineService;

    private final AttendanceInputValidationService attendanceInputValidationService;

    private final MessageService messageService;

    /**
     * 勤怠CSVを全件確認する。
     *
     * @param rows            CSV数据
     * @param targetMonth     导入对象年月
     * @param loginEmployeeNo 登录中的社员编号
     * @return 校验结果
     */
    public AttendanceCsvValidationResult validate(
            List<AttendanceCsvRow> rows,
            YearMonth targetMonth,
            String loginEmployeeNo) {

        if (targetMonth == null) {
            throw new IllegalArgumentException(
                    "targetMonth must not be null");
        }

        if (loginEmployeeNo == null
                || loginEmployeeNo.isBlank()) {

            throw new IllegalArgumentException(
                    "loginEmployeeNo must not be blank");
        }

        /*
         * 期限を過ぎている場合は、
         * CSV内容に関係なく取込不可。
         */
        attendanceDeadlineService.validateEditable(
                targetMonth);

        List<AttendanceCsvValidatedRow> validatedRows = new ArrayList<>();

        List<CsvImportError> errors = new ArrayList<>();

        Set<LocalDate> workDates = new HashSet<>();

        for (AttendanceCsvRow row : rows) {
            validateRow(
                    row,
                    targetMonth,
                    loginEmployeeNo,
                    workDates,
                    validatedRows,
                    errors);
        }

        return new AttendanceCsvValidationResult(
                validatedRows,
                errors);
    }

    /**
     * CSVの1行を確認する。
     *
     * @param row             CSV数据
     * @param targetMonth     导入对象年月
     * @param loginEmployeeNo 登录中的社员编号
     * @param workDates       已读取的工作日期
     * @param validatedRows   校验成功的数据
     * @param errors          错误列表
     */
    private void validateRow(
            AttendanceCsvRow row,
            YearMonth targetMonth,
            String loginEmployeeNo,
            Set<LocalDate> workDates,
            List<AttendanceCsvValidatedRow> validatedRows,
            List<CsvImportError> errors) {

        int errorCountBefore = errors.size();

        validateEmployeeNo(
                row,
                loginEmployeeNo,
                errors);

        LocalDate workDate = parseWorkDate(
                row,
                errors);

        LocalTime attendanceTime = parseTime(
                row.getAttendanceTime(),
                row.getLineNumber(),
                "出勤時刻",
                errors);

        LocalTime leavingTime = parseTime(
                row.getLeavingTime(),
                row.getLineNumber(),
                "退勤時刻",
                errors);

        if (workDate != null) {
            validateTargetMonth(
                    row,
                    workDate,
                    targetMonth,
                    errors);

            validateDuplicateWorkDate(
                    row,
                    workDate,
                    workDates,
                    errors);
        }

        boolean attendanceTimeFormatValid = row.getAttendanceTime() == null
                || row.getAttendanceTime().isBlank()
                || attendanceTime != null;

        boolean leavingTimeFormatValid = row.getLeavingTime() == null
                || row.getLeavingTime().isBlank()
                || leavingTime != null;

        if (attendanceTimeFormatValid
                && leavingTimeFormatValid) {

            validateWorkTypeAndTimes(
                    row,
                    attendanceTime,
                    leavingTime,
                    errors);
        }

        /*
         * この行でエラーが追加されていない場合のみ、
         * 変換済みデータとして保存する。
         */
        if (errors.size() == errorCountBefore) {
            validatedRows.add(
                    new AttendanceCsvValidatedRow(
                            row.getLineNumber(),
                            row.getEmployeeNo(),
                            workDate,
                            attendanceTime,
                            leavingTime,
                            row.getWorkType()));
        }
    }

    /**
     * 社員番号を確認する。
     *
     * @param row             CSV数据
     * @param loginEmployeeNo 登录中的社员编号
     * @param errors          错误列表
     */
    private void validateEmployeeNo(
            AttendanceCsvRow row,
            String loginEmployeeNo,
            List<CsvImportError> errors) {

        if (!loginEmployeeNo.equals(
                row.getEmployeeNo())) {

            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "社員番号",
                            row.getEmployeeNo(),
                            messageService.getMessage(
                                    "scr070.employeeNo.mismatch",
                                    row.getLineNumber())));
        }
    }

    /**
     * 工作日期を変換する。
     *
     * @param row    CSV数据
     * @param errors 错误列表
     * @return 转换后的日期，转换失败时为null
     */
    private LocalDate parseWorkDate(
            AttendanceCsvRow row,
            List<CsvImportError> errors) {

        if (row.getWorkDate() == null
                || row.getWorkDate().isBlank()) {

            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "勤務日",
                            row.getWorkDate(),
                            messageService.getMessage(
                                    "scr070.workDate.required")));

            return null;
        }

        try {
            return LocalDate.parse(
                    row.getWorkDate(),
                    DATE_FORMATTER);

        } catch (DateTimeParseException exception) {

            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "勤務日",
                            row.getWorkDate(),
                            messageService.getMessage(
                                    "scr070.workDate.invalid")));

            return null;
        }
    }

    /**
     * 时刻を変換する。
     *
     * 空文字の場合はnullを返す。
     *
     * @param value      CSV中的值
     * @param lineNumber 行号
     * @param itemName   项目名
     * @param errors     错误列表
     * @return 转换后的时刻
     */
    private LocalTime parseTime(
            String value,
            int lineNumber,
            String itemName,
            List<CsvImportError> errors) {

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(
                    value,
                    TIME_FORMATTER);

        } catch (DateTimeParseException exception) {

            errors.add(
                    new CsvImportError(
                            lineNumber,
                            itemName,
                            value,
                            messageService.getMessage(
                                    "scr070.time.invalid")));

            return null;
        }
    }

    /**
     * 工作日期が对象年月内か確認する。
     *
     * @param row         CSV数据
     * @param workDate    工作日期
     * @param targetMonth 对象年月
     * @param errors      错误列表
     */
    private void validateTargetMonth(
            AttendanceCsvRow row,
            LocalDate workDate,
            YearMonth targetMonth,
            List<CsvImportError> errors) {

        if (!targetMonth.equals(
                YearMonth.from(workDate))) {

            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "勤務日",
                            row.getWorkDate(),
                            messageService.getMessage(
                                    "scr070.workDate.mismatch",
                                    row.getLineNumber())));
        }
    }

    /**
     * 同一工作日期がCSV内で重複していないか確認する。
     *
     * @param row       CSV数据
     * @param workDate  工作日期
     * @param workDates 已读取日期
     * @param errors    错误列表
     */
    private void validateDuplicateWorkDate(
            AttendanceCsvRow row,
            LocalDate workDate,
            Set<LocalDate> workDates,
            List<CsvImportError> errors) {

        if (!workDates.add(workDate)) {
            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "勤務日",
                            row.getWorkDate(),
                            messageService.getMessage(
                                    "scr070.workDate.duplicate",
                                    workDate)));
        }
    }

    /**
     * 勤务区分と出退勤时刻の组合せを確認する。
     *
     * 既存の勤怠入力校验を再利用する。
     *
     * @param row            CSV数据
     * @param attendanceTime 上班时间
     * @param leavingTime    下班时间
     * @param errors         错误列表
     */
    private void validateWorkTypeAndTimes(
            AttendanceCsvRow row,
            LocalTime attendanceTime,
            LocalTime leavingTime,
            List<CsvImportError> errors) {

        try {
            attendanceInputValidationService.validate(
                    row.getWorkType(),
                    attendanceTime,
                    leavingTime);

        } catch (BusinessException exception) {

            errors.add(
                    new CsvImportError(
                            row.getLineNumber(),
                            "勤務区分・出退勤時刻",
                            createWorkTypeAndTimeValue(
                                    row),
                            exception.getMessage()));
        }
    }

    /**
     * 错误显示用の值を作成する。
     *
     * @param row CSV数据
     * @return 勤务区分与时间字符串
     */
    private String createWorkTypeAndTimeValue(
            AttendanceCsvRow row) {

        return String.format(
                "workType=%s, attendanceTime=%s, leavingTime=%s",
                row.getWorkType(),
                row.getAttendanceTime(),
                row.getLeavingTime());
    }
}