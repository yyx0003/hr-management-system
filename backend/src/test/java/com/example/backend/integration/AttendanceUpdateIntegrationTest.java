package com.example.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.attendance.AttendanceUpdateRequest;
import com.example.backend.entity.Attendance;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.service.AttendanceDeadlineService;
import com.example.backend.service.AttendanceUpdateService;
import com.example.backend.service.SalaryCalculationService;
import com.example.backend.service.SalaryResultTransactionHelper;

@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:attendance_update;MODE=PostgreSQL;NON_KEYWORDS=POSITION"
})
@ActiveProfiles("test")
@Sql(scripts = "/sql/initial-employee-test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AttendanceUpdateIntegrationTest {

    private static final Long EMPLOYEE_ID = 1L;
    private static final LocalDate WORK_DATE =
            LocalDate.of(2026, 7, 22);

    @Autowired
    private AttendanceUpdateService attendanceUpdateService;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private SalaryResultTransactionHelper salaryResultTransactionHelper;

    @MockitoBean
    private AttendanceDeadlineService attendanceDeadlineService;

    @MockitoBean
    private SalaryCalculationService salaryCalculationService;

    @BeforeEach
    void setUp() {
        insertAttendance(
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                Attendance.WorkType.NORMAL);
    }

    @Test
    void normalCanBeChangedToPaidLeaveWithNullTimes() {
        updateAttendance(
                null,
                null,
                Attendance.WorkType.PAID_LEAVE);

        Attendance updated = findAttendance();

        assertNull(updated.getAttendanceTime());
        assertNull(updated.getLeavingTime());
        assertEquals(
                Attendance.WorkType.PAID_LEAVE,
                updated.getWorkType());
        verifySalaryRecalculation();
    }

    @Test
    void normalCanBeChangedToAbsenceWithNullTimes() {
        updateAttendance(
                null,
                null,
                Attendance.WorkType.ABSENCE);

        Attendance updated = findAttendance();

        assertNull(updated.getAttendanceTime());
        assertNull(updated.getLeavingTime());
        assertEquals(
                Attendance.WorkType.ABSENCE,
                updated.getWorkType());
        verifySalaryRecalculation();
    }

    @Test
    void paidLeaveCanBeChangedToNormalWithTimes() {
        updateAttendance(
                null,
                null,
                Attendance.WorkType.PAID_LEAVE);

        updateAttendance(
                "08:30",
                "17:30",
                Attendance.WorkType.NORMAL);

        Attendance updated = findAttendance();

        assertEquals(
                LocalTime.of(8, 30),
                updated.getAttendanceTime());
        assertEquals(
                LocalTime.of(17, 30),
                updated.getLeavingTime());
        assertEquals(
                Attendance.WorkType.NORMAL,
                updated.getWorkType());
    }

    @Test
    void absenceCanBeChangedToNormalWithTimes() {
        updateAttendance(
                null,
                null,
                Attendance.WorkType.ABSENCE);

        updateAttendance(
                "09:15",
                "18:15",
                Attendance.WorkType.NORMAL);

        Attendance updated = findAttendance();

        assertEquals(
                LocalTime.of(9, 15),
                updated.getAttendanceTime());
        assertEquals(
                LocalTime.of(18, 15),
                updated.getLeavingTime());
        assertEquals(
                Attendance.WorkType.NORMAL,
                updated.getWorkType());
    }

    private void insertAttendance(
            LocalTime attendanceTime,
            LocalTime leavingTime,
            String workType) {

        Attendance attendance =
                new Attendance();
        attendance.setEmployeeId(EMPLOYEE_ID);
        attendance.setWorkDate(WORK_DATE);
        attendance.setAttendanceTime(attendanceTime);
        attendance.setLeavingTime(leavingTime);
        attendance.setWorkType(workType);

        attendanceRepository.insert(attendance);
    }

    private void updateAttendance(
            String attendanceTime,
            String leavingTime,
            String workType) {

        AttendanceUpdateRequest request =
                new AttendanceUpdateRequest();
        request.setAttendanceTime(attendanceTime);
        request.setLeavingTime(leavingTime);
        request.setWorkType(workType);

        attendanceUpdateService.update(
                EMPLOYEE_ID,
                WORK_DATE,
                request);
    }

    private Attendance findAttendance() {
        return attendanceRepository.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(
                                Attendance::getEmployeeId,
                                EMPLOYEE_ID)
                        .eq(
                                Attendance::getWorkDate,
                                WORK_DATE));
    }

    private void verifySalaryRecalculation() {
        verify(salaryCalculationService)
                .recalculateForEmployeeSafely(
                        salaryResultTransactionHelper,
                        EMPLOYEE_ID,
                        YearMonth.of(2026, 7));
    }
}
