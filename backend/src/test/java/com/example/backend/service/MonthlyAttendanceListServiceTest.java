package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.example.backend.dto.attendance.AttendanceListItem;
import com.example.backend.dto.attendance.AttendanceListResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Holiday;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.HolidayRepository;

@ExtendWith(MockitoExtension.class)
class MonthlyAttendanceListServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private HolidayRepository holidayRepository;

    private MonthlyAttendanceListService service;

    @BeforeEach
    void setUp() {

        service =
                new MonthlyAttendanceListService(
                        attendanceRepository,
                        holidayRepository,
                        new MonthlyDateService(),
                        new AttendanceListItemMapper());
    }

    @Test
    void returnsAllDatesForTargetMonth() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        LocalDate startDate =
                LocalDate.of(2026, 7, 1);

        LocalDate endDate =
                LocalDate.of(2026, 7, 31);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        startDate,
                        endDate))
                .thenReturn(List.of());

        when(holidayRepository.findByDateRange(
                startDate,
                endDate))
                .thenReturn(List.of());

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        assertEquals(
                "2026-07",
                result.getTargetMonth());

        assertEquals(
                31,
                result.getAttendanceList().size());

        assertEquals(
                "2026-07-01",
                result.getAttendanceList()
                        .get(0)
                        .getWorkDate());

        assertEquals(
                "2026-07-31",
                result.getAttendanceList()
                        .get(30)
                        .getWorkDate());

        verify(attendanceRepository)
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        startDate,
                        endDate);

        verify(holidayRepository)
                .findByDateRange(
                        startDate,
                        endDate);
    }

    @Test
    void registeredAttendanceIsIncluded() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        LocalDate workDate =
                LocalDate.of(2026, 7, 1);

        Attendance attendance =
                createAttendance(
                        employeeId,
                        workDate,
                        LocalTime.of(9, 0),
                        LocalTime.of(18, 0),
                        Attendance.WorkType.NORMAL);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetYearMonth.atDay(1),
                        targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of(attendance));

        when(holidayRepository.findByDateRange(
                targetYearMonth.atDay(1),
                targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        AttendanceListItem item =
                findItem(
                        result,
                        "2026-07-01");

        assertEquals(
                "09:00",
                item.getAttendanceTime());

        assertEquals(
                "18:00",
                item.getLeavingTime());

        assertEquals(
                Attendance.WorkType.NORMAL,
                item.getWorkType());

        assertNull(item.getHolidayType());
        assertNull(item.getHolidayName());
    }

    @Test
    void holidayMasterIsIncluded() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        LocalDate holidayDate =
                LocalDate.of(2026, 7, 20);

        Holiday holiday =
                createHoliday(
                        holidayDate,
                        "HOLIDAY",
                        "海の日");

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetYearMonth.atDay(1),
                        targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        when(holidayRepository.findByDateRange(
                targetYearMonth.atDay(1),
                targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of(holiday));

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        AttendanceListItem item =
                findItem(
                        result,
                        "2026-07-20");

        assertEquals(
                "HOLIDAY",
                item.getHolidayType());

        assertEquals(
                "海の日",
                item.getHolidayName());
    }

    @Test
    void weekendIsIncludedWhenHolidayMasterDoesNotExist() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetYearMonth.atDay(1),
                        targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        when(holidayRepository.findByDateRange(
                targetYearMonth.atDay(1),
                targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        AttendanceListItem saturday =
                findItem(
                        result,
                        "2026-07-04");

        assertEquals(
                "WEEKEND",
                saturday.getHolidayType());

        assertEquals(
                "土曜日",
                saturday.getHolidayName());

        AttendanceListItem sunday =
                findItem(
                        result,
                        "2026-07-05");

        assertEquals(
                "WEEKEND",
                sunday.getHolidayType());

        assertEquals(
                "日曜日",
                sunday.getHolidayName());
    }

    @Test
    void holidayMasterTakesPriorityOverWeekend() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        LocalDate sundayDate =
                LocalDate.of(2026, 7, 5);

        Holiday holiday =
                createHoliday(
                        sundayDate,
                        "SUMMER",
                        "夏季休暇");

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetYearMonth.atDay(1),
                        targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        when(holidayRepository.findByDateRange(
                targetYearMonth.atDay(1),
                targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of(holiday));

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        AttendanceListItem item =
                findItem(
                        result,
                        "2026-07-05");

        assertEquals(
                "SUMMER",
                item.getHolidayType());

        assertEquals(
                "夏季休暇",
                item.getHolidayName());
    }

    @Test
    void paidLeaveWithoutTimesIsIncluded() {

        Long employeeId = 1L;
        YearMonth targetYearMonth =
                YearMonth.of(2026, 7);

        LocalDate workDate =
                LocalDate.of(2026, 7, 24);

        Attendance attendance =
                createAttendance(
                        employeeId,
                        workDate,
                        null,
                        null,
                        Attendance.WorkType.PAID_LEAVE);

        when(attendanceRepository
                .findByEmployeeIdAndTargetMonth(
                        employeeId,
                        targetYearMonth.atDay(1),
                        targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of(attendance));

        when(holidayRepository.findByDateRange(
                targetYearMonth.atDay(1),
                targetYearMonth.atEndOfMonth()))
                .thenReturn(List.of());

        AttendanceListResponse result =
                service.getMonthlyAttendanceList(
                        employeeId,
                        targetYearMonth);

        AttendanceListItem item =
                findItem(
                        result,
                        "2026-07-24");

        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                item.getWorkType());

        assertNull(item.getAttendanceTime());
        assertNull(item.getLeavingTime());
    }

    @Test
    void nullEmployeeIdThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.getMonthlyAttendanceList(
                                null,
                                YearMonth.of(2026, 7)));

        assertEquals(
                "employeeId must not be null",
                exception.getMessage());
    }

    @Test
    void nullTargetYearMonthThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> service.getMonthlyAttendanceList(
                                1L,
                                null));

        assertEquals(
                "targetYearMonth must not be null",
                exception.getMessage());
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

    private Holiday createHoliday(
            LocalDate holidayDate,
            String holidayType,
            String holidayName) {

        Holiday holiday =
                new Holiday();

        holiday.setHolidayDate(holidayDate);
        holiday.setHolidayType(holidayType);
        holiday.setHolidayName(holidayName);

        return holiday;
    }

    private AttendanceListItem findItem(
            AttendanceListResponse response,
            String workDate) {

        return response.getAttendanceList()
                .stream()
                .filter(item ->
                        workDate.equals(
                                item.getWorkDate()))
                .findFirst()
                .orElseThrow();
    }
}