package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 社員（employeeテーブル対応）
 * employee_id + start_date の複合主キー。1人の社員が複数行（部署・職能資格・役職の変更履歴分）を持つ。
 *
 * 【重要】DB上の実際の主キーはemployee_id + start_dateの複合キーだが、
 * employeeIdはBIGSERIAL（自動採番）のため、insert()実行後に生成値をJavaオブジェクトへ
 * 書き戻すためだけに@TableId(type = IdType.AUTO)を付与している。
 * これにより、BaseMapperのselectById・updateById・deleteByIdが「コンパイルは通る」
 * 状態になるが、複合キーの一部（employeeId単独）でしか絞り込めず、意図しない挙動になる。
 * これら3メソッドは絶対に呼び出さないこと。検索・更新・削除は全てEmployeeRepositoryの
 * カスタムSQL（@Select・@Delete等）で行う。
 *
 * 【重要】新規社員登録時はemployeeIdをnullのままinsertしてよい（DBが自動採番する）。
 * ただし、既存社員の履歴行を追加する場合（部署・役職・職能資格の変更）は、
 * 必ず既存のemployeeIdを明示的にセットしてからinsertすること。
 * 指定を忘れると新しいIDが自動生成され、同一人物が別人として扱われてしまう
 * （エラーにはならないため、実装・レビュー時に特に注意すること）。
 */
@TableName("employee")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    /**
     * DBのBIGSERIAL。新規登録時はnullのままinsert()してよい
     * （@TableId(type = IdType.AUTO)により、insert後に生成値が自動的にこのフィールドへ書き戻される）。
     * 履歴行追加時は既存値を明示的にセットしてからinsert()すること。
     */
    @TableId(value = "employee_id", type = IdType.AUTO)
    private Long employeeId;

    private LocalDate startDate;
    private String employeeNo;
    private String passwordHash;
    private String employeeName;
    private LocalDate birthDate;
    private String postalCode;
    private String address;
    private String phoneNumber;
    private String emailAddress;
    private LocalDate hireDate;

    /** 在籍中はnull。この列の有無で在籍／退職を判定する。 */
    private LocalDate retireDate;

    /** 対象期間における所属部署ID（外部キーなし） */
    private Long departmentId;

    /** 対象期間における職能資格の等級（1〜10） */
    private Integer skillGrade;

    /** 対象期間における役職ID（役職なしはnull） */
    private Long positionId;

    /** この行の適用終了日。現在有効な行はnull。 */
    private LocalDate endDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 在籍中かどうかを判定する。 */
    public boolean isActive() {
        return retireDate == null;
    }
}