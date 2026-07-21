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
 * 職能資格（skill_gradeテーブル対応）
 * skill_grade（等級） + start_date の複合主キーで現在値・履歴を一体管理する。
 * 等級は1〜10の範囲（DB側でCHECK制約あり）。
 */
@TableName("skill_grade")
@Getter
@Setter
@NoArgsConstructor
public class SkillGrade {

    /** 等級（1〜10） */
    private Integer skillGrade;

    private LocalDate startDate;
    private Long allowance;
    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
