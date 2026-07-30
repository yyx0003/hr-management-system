package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SalaryResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SalaryResultRepository extends BaseMapper<SalaryResult> {

    /** insert()はBaseMapperの標準メソッドをそのまま使用可（主キー制約の影響を受けないため）。 */

    @Delete("DELETE FROM salary_result WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);

    /** 対象年月の給与実績を社員ID順で取得する。 */
    @Select("""
            SELECT * FROM salary_result
            WHERE target_year = #{targetYear}
            AND target_month = #{targetMonth}
            ORDER BY employee_id
            """)
    List<SalaryResult> findByTargetYearAndTargetMonthOrderByEmployeeId(
            @Param("targetYear") Integer targetYear,
            @Param("targetMonth") Integer targetMonth);
}
