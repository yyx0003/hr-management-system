package com.example.backend.dto.attendance;
 
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AllArgsConstructor;
import lombok.Data;
 
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
     * 残業時間・休日勤務時間。
     */
    private BigDecimal overtimeHours;
 
    /**
     * 0時間の結果を返す。
     */
    public static WorkHoursResult zero() {
        BigDecimal zero = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        return new WorkHoursResult(zero, zero);
    }
 
    /**
     * 勤務時間を加算する。
     */
    public WorkHoursResult add(WorkHoursResult other) {
        if (other == null) {
            return this;
        }
 
        BigDecimal currentWorkHours =
                workHours == null ? BigDecimal.ZERO : workHours;
 
        BigDecimal currentOvertimeHours =
                overtimeHours == null ? BigDecimal.ZERO : overtimeHours;
 
        BigDecimal otherWorkHours =
                other.getWorkHours() == null
                        ? BigDecimal.ZERO
                        : other.getWorkHours();
 
        BigDecimal otherOvertimeHours =
                other.getOvertimeHours() == null
                        ? BigDecimal.ZERO
                        : other.getOvertimeHours();
 
        return new WorkHoursResult(
                currentWorkHours.add(otherWorkHours).setScale(SCALE, ROUNDING),
                currentOvertimeHours.add(otherOvertimeHours).setScale(SCALE, ROUNDING));
    }
}