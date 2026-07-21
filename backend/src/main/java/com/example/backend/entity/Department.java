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
 * 部署（departmentテーブル対応）
 * department_id + start_date の複合主キーで現在値・履歴を一体管理する。
 * department_idは業務側で採番するBIGINT（自動採番ではない）。
 * 複合主キーのため、@TableId／BaserepositoryのID系メソッドは使用しない。
 */
@TableName("department")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    private Long departmentId;
    private LocalDate startDate;
    private String departmentName;

    /** 部署変更は月初（1日）からのみ適用可能。現在有効な行はnull。 */
    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
