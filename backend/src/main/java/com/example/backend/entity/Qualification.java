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
 * 資格（qualificationテーブル対応）
 * qualification_id + start_date の複合主キーで現在値・履歴を一体管理する。
 */
@TableName("qualification")
@Getter
@Setter
@NoArgsConstructor
public class Qualification {

    private Long qualificationId;
    private LocalDate startDate;
    private String qualificationName;

    /** 高度資格区分。trueの場合、2件目以降は加算額が異なる（サービス側で判定）。 */
    private Boolean isAdvance;

    private Long qualificationAllowance;
    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
