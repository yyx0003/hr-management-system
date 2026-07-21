package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 祝日マスタ（holidayテーブル対応）
 * holiday_type の値: HOLIDAY（国民の祝日）／SUMMER（夏季休暇）／WINTER（冬期休暇）
 * ※holiday_2026.sqlで実際に使用されている値と一致させること。
 */
@TableName("holiday")
@Getter
@Setter
@NoArgsConstructor
public class Holiday {

    private LocalDate holidayDate;
    private String holidayType;
    private String holidayName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
