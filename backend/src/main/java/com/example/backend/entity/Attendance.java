package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 勤怠データ（attendanceテーブル対応）
 * employee_id + work_date の複合主キー。
 *
 * 【注意】work_typeはDB側でNOT NULL（デフォルト値なし）。アプリ側で必ず明示的に値をセットすること。
 * NORMAL・HOLIDAY_WORKはattendance_time／leaving_timeが両方必須、
 * PAID_LEAVE・ABSENCEは両方null必須（DB側のCHECK制約）。
 *
 * work_typeはString型で保持する（MyBatis-Plusでenumを直接使う場合は@EnumValue等の
 * 追加設定が必要なため、シンプルにするためString＋定数で扱う）。
 * 値: NORMAL / PAID_LEAVE / ABSENCE / HOLIDAY_WORK
 */
@TableName("attendance")
@Getter
@Setter
@NoArgsConstructor
public class Attendance {

    private Long employeeId;
    private LocalDate workDate;
    private LocalTime attendanceTime;
    private LocalTime leavingTime;
    private String workType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** work_typeの定数（DB側のCHECK制約と一致させること） */
    public static final class WorkType {
        public static final String NORMAL = "NORMAL";
        public static final String PAID_LEAVE = "PAID_LEAVE";
        public static final String ABSENCE = "ABSENCE";
        public static final String HOLIDAY_WORK = "HOLIDAY_WORK";

        private WorkType() {
        }
    }
}
