package com.example.backend.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.dto.attendance.AttendanceListResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Holiday;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;

/**
 * 月単位の勤怠一覧を取得するサービス。
 */
@Service
@RequiredArgsConstructor
public class MonthlyAttendanceListService {

    private static final DateTimeFormatter TARGET_MONTH_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM");

    private static final String WEEKEND_TYPE = "WEEKEND";
    private static final String SATURDAY_NAME = "土曜日";
    private static final String SUNDAY_NAME = "日曜日";

    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
    private final MonthlyDateService monthlyDateService;
    private final AttendanceListItemMapper attendanceListItemMapper;

    /**
     * 対象社員・対象年月の勤怠一覧を取得する。
     *
     * 勤怠が登録されていない日も含め、
     * 対象月の1日から月末までを全て返す。
     *
     * @param employeeId 社員ID
     * @param targetYearMonth 対象年月
     * @return 月単位の勤怠一覧
     */
    public AttendanceListResponse getMonthlyAttendanceList(
            Long employeeId,
            YearMonth targetYearMonth) {

        validateArguments(employeeId, targetYearMonth);

        LocalDate targetMonthStart =
                targetYearMonth.atDay(1);

        LocalDate targetMonthEnd =
                targetYearMonth.atEndOfMonth();

        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetMonthStart,
                        targetMonthEnd);

        List<Holiday> holidayList =
                holidayRepository.findByDateRange(
                        targetMonthStart,
                        targetMonthEnd);

        Map<LocalDate, Attendance> attendanceMap =
                attendanceList.stream()
                        .collect(
                                Collectors.toMap(
                                        Attendance::getWorkDate,
                                        Function.identity()));

        Map<LocalDate, Holiday> holidayMap =
                holidayList.stream()
                        .collect(
                                Collectors.toMap(
                                        Holiday::getHolidayDate,
                                        Function.identity()));

        List<AttendanceListItem> items =
                monthlyDateService.createDates(targetYearMonth)
                        .stream()
                        .map(workDate -> createListItem(
                                workDate,
                                attendanceMap.get(workDate),
                                holidayMap.get(workDate)))
                        .toList();

        return new AttendanceListResponse(
                targetYearMonth.format(
                        TARGET_MONTH_FORMATTER),
                items);
    }

    /**
     * 1日分の勤怠一覧DTOを生成する。
     *
     * @param workDate 対象日
     * @param attendance 勤怠データ
     * @param holiday 休日データ
     * @return 1日分の勤怠一覧DTO
     */
    private AttendanceListItem createListItem(
            LocalDate workDate,
            Attendance attendance,
            Holiday holiday) {

        Holiday displayHoliday = holiday;

        if (displayHoliday == null) {
            displayHoliday =
                    createWeekendHoliday(workDate);
        }

        return attendanceListItemMapper.toListItem(
                workDate,
                attendance,
                displayHoliday);
    }

    /**
     * 土曜日・日曜日の表示用休日データを生成する。
     *
     * 平日の場合はnullを返す。
     *
     * @param workDate 対象日
     * @return 表示用休日データ
     */
    private Holiday createWeekendHoliday(
            LocalDate workDate) {

        DayOfWeek dayOfWeek =
                workDate.getDayOfWeek();

        if (dayOfWeek != DayOfWeek.SATURDAY
                && dayOfWeek != DayOfWeek.SUNDAY) {
            return null;
        }

        Holiday weekend = new Holiday();
        weekend.setHolidayDate(workDate);
        weekend.setHolidayType(WEEKEND_TYPE);
        weekend.setHolidayName(
                dayOfWeek == DayOfWeek.SATURDAY
                        ? SATURDAY_NAME
                        : SUNDAY_NAME);

        return weekend;
    }

    /**
     * 引数を確認する。
     *
     * @param employeeId 社員ID
     * @param targetYearMonth 対象年月
     */
    private void validateArguments(
            Long employeeId,
            YearMonth targetYearMonth) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "employeeId must not be null");
        }

        if (targetYearMonth == null) {
            throw new IllegalArgumentException(
                    "targetYearMonth must not be null");
        }
    }
}