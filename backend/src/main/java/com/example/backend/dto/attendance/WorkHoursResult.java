package com.example.backend.dto.attendance;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 勤務時間・残業時間・休日出勤時間・不足時間の計算結果。
 */
@Data
@AllArgsConstructor
public class WorkHoursResult {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * 通常勤務時間。
     */
    private BigDecimal workHours;

    /**
     * 残業時間（NORMALで8時間を超過した分）。
     */
    private BigDecimal overtimeHours;

    /**
     * 休日出勤時間（HOLIDAY_WORKの実働時間）。
     */
    private BigDecimal holidayWorkHours;

    /**
     * 不足時間（欠勤・遅刻・早退により標準勤務時間に満たない分の合計）。
     */
    private BigDecimal shortfallHours;

    /**
     * 全項目が0の結果を返す。
     *
     * @return 0時間の計算結果
     */
    public static WorkHoursResult zero() {
        BigDecimal zero = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        return new WorkHoursResult(zero, zero, zero, zero);
    }

    /**
     * 指定された勤務時間を加算する。
     *
     * @param other 加算する勤務時間
     * @return 加算後の計算結果
     */
    public WorkHoursResult add(WorkHoursResult other) {
        if (other == null) {
            return this;
        }

        BigDecimal currentWorkHours = workHours == null ? BigDecimal.ZERO : workHours;
        BigDecimal currentOvertimeHours = overtimeHours == null ? BigDecimal.ZERO : overtimeHours;
        BigDecimal currentHolidayWorkHours = holidayWorkHours == null ? BigDecimal.ZERO : holidayWorkHours;
        BigDecimal currentShortfallHours = shortfallHours == null ? BigDecimal.ZERO : shortfallHours;

        BigDecimal otherWorkHours = other.getWorkHours() == null ? BigDecimal.ZERO : other.getWorkHours();
        BigDecimal otherOvertimeHours = other.getOvertimeHours() == null ? BigDecimal.ZERO : other.getOvertimeHours();
        BigDecimal otherHolidayWorkHours = other.getHolidayWorkHours() == null ? BigDecimal.ZERO : other.getHolidayWorkHours();
        BigDecimal otherShortfallHours = other.getShortfallHours() == null ? BigDecimal.ZERO : other.getShortfallHours();

        return new WorkHoursResult(
                currentWorkHours.add(otherWorkHours).setScale(SCALE, ROUNDING),
                currentOvertimeHours.add(otherOvertimeHours).setScale(SCALE, ROUNDING),
                currentHolidayWorkHours.add(otherHolidayWorkHours).setScale(SCALE, ROUNDING),
                currentShortfallHours.add(otherShortfallHours).setScale(SCALE, ROUNDING));
    }
}