package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.common.MessageService;
import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Holiday;

class AttendanceListItemMapperTest {

    private AttendanceListItemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper =
                new AttendanceListItemMapper(
                        new AttendanceService(
                                mock(MessageService.class)));
    }

    @Test
    void attendanceAndHolidayAreMapped() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 20);

        Attendance attendance = new Attendance();
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(LocalTime.of(9, 0));
        attendance.setLeavingTime(LocalTime.of(18, 0));
        attendance.setWorkType(Attendance.WorkType.NORMAL);

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(workDate);
        holiday.setHolidayType("HOLIDAY");
        holiday.setHolidayName("海の日");

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        attendance,
                        holiday);

        assertEquals(
                "2026-07-20",
                result.getWorkDate());

        assertEquals(
                "09:00",
                result.getAttendanceTime());

        assertEquals(
                "18:00",
                result.getLeavingTime());

        assertEquals(
                Attendance.WorkType.NORMAL,
                result.getWorkType());

        assertEquals(
                "HOLIDAY",
                result.getHolidayType());

        assertEquals(
                "海の日",
                result.getHolidayName());

        assertNull(result.getActualWorkHours());
    }

    @Test
    void attendanceWithoutHolidayIsMapped() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 22);

        Attendance attendance = new Attendance();
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(LocalTime.of(9, 30));
        attendance.setLeavingTime(LocalTime.of(18, 15));
        attendance.setWorkType(Attendance.WorkType.NORMAL);

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        attendance,
                        null);

        assertEquals(
                "2026-07-22",
                result.getWorkDate());

        assertEquals(
                "09:30",
                result.getAttendanceTime());

        assertEquals(
                "18:15",
                result.getLeavingTime());

        assertEquals(
                Attendance.WorkType.NORMAL,
                result.getWorkType());

        assertNull(result.getHolidayType());
        assertNull(result.getHolidayName());

        assertEquals(
                new BigDecimal("8.75"),
                result.getActualWorkHours());
    }

    @Test
    void holidayWithoutAttendanceIsMapped() {

        LocalDate workDate =
                LocalDate.of(2026, 8, 11);

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(workDate);
        holiday.setHolidayType("HOLIDAY");
        holiday.setHolidayName("山の日");

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        null,
                        holiday);

        assertEquals(
                "2026-08-11",
                result.getWorkDate());

        assertNull(result.getAttendanceTime());
        assertNull(result.getLeavingTime());
        assertNull(result.getWorkType());

        assertEquals(
                "HOLIDAY",
                result.getHolidayType());

        assertEquals(
                "山の日",
                result.getHolidayName());
    }

    @Test
    void paidLeaveWithoutTimesIsMapped() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 24);

        Attendance attendance = new Attendance();
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(null);
        attendance.setLeavingTime(null);
        attendance.setWorkType(
                Attendance.WorkType.PAID_LEAVE);

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        attendance,
                        null);

        assertEquals(
                "2026-07-24",
                result.getWorkDate());

        assertNull(result.getAttendanceTime());
        assertNull(result.getLeavingTime());

        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                result.getWorkType());

        assertNull(result.getActualWorkHours());
    }

    @Test
    void dateWithoutAttendanceAndHolidayIsMapped() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 25);

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        null,
                        null);

        assertEquals(
                "2026-07-25",
                result.getWorkDate());

        assertNull(result.getAttendanceTime());
        assertNull(result.getLeavingTime());
        assertNull(result.getWorkType());
        assertNull(result.getHolidayType());
        assertNull(result.getHolidayName());
        assertNull(result.getActualWorkHours());
    }

    @Test
    void holidayWorkUsesOvertimeHoursAsActualWorkHours() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 26);

        Attendance attendance = new Attendance();
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(LocalTime.of(9, 0));
        attendance.setLeavingTime(LocalTime.of(17, 30));
        attendance.setWorkType(
                Attendance.WorkType.HOLIDAY_WORK);

        Holiday holiday = new Holiday();
        holiday.setHolidayDate(workDate);
        holiday.setHolidayType("WEEKEND");
        holiday.setHolidayName("日曜日");

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        attendance,
                        holiday);

        assertEquals(
                new BigDecimal("8.50"),
                result.getActualWorkHours());
    }

    @Test
    void incompleteWorkTimesHaveNoActualWorkHours() {

        LocalDate workDate =
                LocalDate.of(2026, 7, 27);

        Attendance attendance = new Attendance();
        attendance.setWorkDate(workDate);
        attendance.setAttendanceTime(LocalTime.of(9, 0));
        attendance.setLeavingTime(null);
        attendance.setWorkType(Attendance.WorkType.NORMAL);

        AttendanceListItem result =
                mapper.toListItem(
                        workDate,
                        attendance,
                        null);

        assertNull(result.getActualWorkHours());
    }

    @Test
    void nullWorkDateThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> mapper.toListItem(
                                null,
                                null,
                                null));

        assertEquals(
                "workDate must not be null",
                exception.getMessage());
    }
}
