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
 * 社員取得資格（employee_qualificationテーブル対応）
 * employee_id + qualification_id の複合主キー（同一資格を複数保持することはない前提）。
 */
@TableName("employee_qualification")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeQualification {

    private Long employeeId;
    private Long qualificationId;
    private LocalDate acquisitionDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
