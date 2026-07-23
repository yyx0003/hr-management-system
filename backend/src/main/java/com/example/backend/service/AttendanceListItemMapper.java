package com.example.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Holiday;

/**
 * 勤怠データを一覧表示用DTOへ変換するクラス。
 */
@Component
public class AttendanceListItemMapper {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 勤怠データと休日データを一覧表示用DTOへ変換する。
     *
     * 勤怠未登録日や休日以外の日は、該当項目をnullで返す。
     *
     * @param workDate 対象日
     * @param attendance 勤怠データ
     * @param holiday 休日データ
     * @return 勤怠一覧の1日分
     */
    public AttendanceListItem toListItem(
            LocalDate workDate,
            Attendance attendance,
            Holiday holiday) {

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "workDate must not be null");
        }

        return new AttendanceListItem(
                formatDate(workDate),
                attendance == null
                        ? null
                        : formatTime(attendance.getAttendanceTime()),
                attendance == null
                        ? null
                        : formatTime(attendance.getLeavingTime()),
                attendance == null
                        ? null
                        : attendance.getWorkType(),
                holiday == null
                        ? null
                        : holiday.getHolidayType(),
                holiday == null
                        ? null
                        : holiday.getHolidayName());
    }

    /**
     * 日付をyyyy-MM-dd形式へ変換する。
     *
     * @param date 日付
     * @return 変換後の日付
     */
    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * 時刻をHH:mm形式へ変換する。
     *
     * @param time 時刻
     * @return 変換後の時刻
     */
    private String formatTime(LocalTime time) {

        if (time == null) {
            return null;
        }

        return time.format(TIME_FORMATTER);
    }
}