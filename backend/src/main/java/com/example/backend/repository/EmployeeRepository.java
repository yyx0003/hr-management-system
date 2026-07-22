package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Employee;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 【注意】employeeは複合主キー（employee_id + start_date）のため、
 * BaseMapperのselectById／updateById／deleteByIdは使用しないこと
 * （単一キー前提のため、意図しない挙動になる可能性がある）。
 * insert()は主キーの制約を受けないため使用可。検索・削除は全てカスタムSQLで行う。
 */
@Mapper
public interface EmployeeRepository extends BaseMapper<Employee> {

    /**
     * 対象年月に在籍していた社員のemployee_idを、重複を除いて取得する（給与計算対象）。
     * さらに、対象年月分のsalary_resultが未登録の社員のみを対象とする
     * （再実行時の重複登録・重複計算を防ぐため）。
     */
    @Select("""
            SELECT DISTINCT e.employee_id FROM employee e
            WHERE e.hire_date <= #{targetMonthEnd}
            AND (e.retire_date IS NULL OR e.retire_date >= #{targetMonthStart})
            AND NOT EXISTS (
                SELECT 1 FROM salary_result sr
                WHERE sr.employee_id = e.employee_id
                AND sr.target_year = #{targetYear} AND sr.target_month = #{targetMonth}
            )
            ORDER BY e.employee_id
            """)
    List<Long> findEligibleEmployeeIds(@Param("targetMonthStart") LocalDate targetMonthStart,
                                        @Param("targetMonthEnd") LocalDate targetMonthEnd,
                                        @Param("targetYear") Integer targetYear,
                                        @Param("targetMonth") Integer targetMonth);

    /**
     * 退職後、基準日以前に退職した社員のemployee_idを、重複を除いて取得する（退職者削除バッチ対象）。
     */
    @Select("""
            SELECT DISTINCT employee_id FROM employee
            WHERE retire_date IS NOT NULL AND retire_date <= #{cutoffDate}
            ORDER BY employee_id
            """)
    List<Long> findRetireeIds(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 対象社員の対象年月末日時点で有効な行を1件取得する。
     * この1行にdepartment_id・skill_grade・position_idがまとめて含まれる。
     */
    @Select("""
            SELECT * FROM employee
            WHERE employee_id = #{employeeId}
            AND start_date <= #{targetMonthEnd}
            AND (end_date IS NULL OR end_date >= #{targetMonthEnd})
            """)
    Employee findEffectiveAt(@Param("employeeId") Long employeeId,
                              @Param("targetMonthEnd") LocalDate targetMonthEnd);

    /**
     * 基準日時点で有効かつ在職中の社員履歴を社員番号で取得する。
     * 退職日当日は在職中として扱う。
     */
    @Select("""
            SELECT * FROM employee
            WHERE employee_no = #{employeeNo}
            AND start_date <= #{referenceDate}
            AND (end_date IS NULL OR end_date >= #{referenceDate})
            AND (retire_date IS NULL OR retire_date >= #{referenceDate})
            """)
    Employee findEffectiveAndEmployedByEmployeeNoAt(@Param("employeeNo") String employeeNo,
                                                     @Param("referenceDate") LocalDate referenceDate);

    /** 対象employee_idの全履歴行を取得する。 */
    @Select("SELECT * FROM employee WHERE employee_id = #{employeeId}")
    List<Employee> findByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 対象employee_idの全履歴行を削除する（start_dateを条件に含めないため全行が対象）。
     * 退職者削除バッチで使用する。
     */
    @Delete("DELETE FROM employee WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Select("SELECT COUNT(*) > 0 FROM employee WHERE employee_no = #{employeeNo}")
    boolean existsByEmployeeNo(@Param("employeeNo") String employeeNo);
}
