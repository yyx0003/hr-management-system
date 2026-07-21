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
 * 役職（"position"テーブル対応）
 * position_id + start_date の複合主キーで現在値・履歴を一体管理する。
 *
 * 【注意】DB側でテーブル名は "position"（ダブルクォート付き）で作成されている
 * （SQLのPOSITION関数と紛らわしいため、念のため引用識別子にしている）。
 * MyBatis-PlusのSQLは@TableName値をそのままFROM句に使うため、repository側の
 * カスタムSQLでも同様にダブルクォートで囲む必要がある。
 */
@TableName("\"position\"")
@Getter
@Setter
@NoArgsConstructor
public class Position {

    private Long positionId;
    private LocalDate startDate;
    private String positionName;
    private Long positionAllowance;
    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
