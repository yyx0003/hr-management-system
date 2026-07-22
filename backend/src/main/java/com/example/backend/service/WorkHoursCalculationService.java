package com.example.backend.service;
 
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.backend.controller.SalaryConstants;
import com.example.backend.dto.attendance.WorkHoursResult;
import com.example.backend.entity.Attendance;
import com.example.backend.entity.Attendance.WorkType;
import com.example.backend.entity.Holiday;
import com.example.backend.repository.AttendanceRepository;
import com.example.backend.repository.HolidayRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
/**
* 勤怠実績から稼働時間・残業時間を算出する共通ロジック。
*
* 稼働時間・残業時間はBigDecimal（スケール2、四捨五入）で計算する。
* 出勤時刻が何時であっても補正は行わない（早出分もそのまま実働時間に含める）。
*/
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkHoursCalculationService {
 
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
 
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;
 
    /**
     * 対象社員・対象年月の稼働時間・残業時間を集計する。
     * NORMAL：出退勤時刻から実働時間をそのまま算出し、8時間を超えた分を残業時間とする。
     * HOLIDAY_WORK：実働時間を全て残業時間として計上する。
     * PAID_LEAVE：実働8時間とみなす。ABSENCE：実働0とする。
     */
    public WorkHoursResult calculateWorkHours(Long employeeId, LocalDate targetMonthStart, LocalDate targetMonthEnd) {
        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeIdAndTargetMonth(employeeId, targetMonthStart, targetMonthEnd);
 
        WorkHoursResult total = WorkHoursResult.zero();
        for (Attendance attendance : attendanceList) {
            total = total.add(calculateDailyHours(attendance));
        }
        return total;
    }
 
    private WorkHoursResult calculateDailyHours(Attendance attendance) {
        String workType = attendance.getWorkType();
 
        if (WorkType.NORMAL.equals(workType)) {
            BigDecimal hours = actualHours(attendance);
            BigDecimal overtime = hours.subtract(SalaryConstants.STANDARD_WORK_HOURS)
                    .max(BigDecimal.ZERO.setScale(SCALE, ROUNDING));
            BigDecimal regular = hours.subtract(overtime);
            return new WorkHoursResult(regular, overtime);
        }
        if (WorkType.HOLIDAY_WORK.equals(workType)) {
            return new WorkHoursResult(BigDecimal.ZERO.setScale(SCALE, ROUNDING), actualHours(attendance));
        }
        if (WorkType.PAID_LEAVE.equals(workType)) {
            return new WorkHoursResult(SalaryConstants.STANDARD_WORK_HOURS, BigDecimal.ZERO.setScale(SCALE, ROUNDING));
        }
        // ABSENCE、その他未定義値は実働0として扱う
        return WorkHoursResult.zero();
    }
 
    private BigDecimal actualHours(Attendance attendance) {
        if (attendance.getAttendanceTime() == null || attendance.getLeavingTime() == null) {
            log.warn("出勤・退勤時刻が未入力です。employeeId={}, workDate={}",
                    attendance.getEmployeeId(), attendance.getWorkDate());
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        Duration duration = Duration.between(attendance.getAttendanceTime(), attendance.getLeavingTime());
        return BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), SCALE, ROUNDING);
    }
 
    /**
     * work_type=HOLIDAY_WORKの日が実際に休日（土日・祝日・夏季／冬期休暇）に該当するか検証する。
     * 不一致の場合は警告ログのみ出力し、処理は継続する（給与計算結果には影響しない）。
     */
    public void validateHolidayWork(Long employeeId, LocalDate targetMonthStart, LocalDate targetMonthEnd) {
        List<Attendance> attendanceList =
                attendanceRepository.findByEmployeeIdAndTargetMonth(employeeId, targetMonthStart, targetMonthEnd);
        List<Holiday> holidays = holidayRepository.findByDateRange(targetMonthStart, targetMonthEnd);
        Set<LocalDate> holidayDates = new HashSet<>();
        for (Holiday holiday : holidays) {
            holidayDates.add(holiday.getHolidayDate());
        }
 
        for (Attendance attendance : attendanceList) {
            if (!WorkType.HOLIDAY_WORK.equals(attendance.getWorkType())) {
                continue;
            }
            LocalDate workDate = attendance.getWorkDate();
            boolean isWeekend = workDate.getDayOfWeek() == DayOfWeek.SATURDAY
                    || workDate.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean isHoliday = holidayDates.contains(workDate);
            if (!isWeekend && !isHoliday) {
                log.warn("HOLIDAY_WORKですが休日に該当しません。employeeId={}, workDate={}", employeeId, workDate);
            }
        }
    }
}