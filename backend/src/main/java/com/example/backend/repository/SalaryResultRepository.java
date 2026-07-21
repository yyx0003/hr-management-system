package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SalaryResult;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SalaryResultRepository extends BaseMapper<SalaryResult> {

    /** insert()はBaseMapperの標準メソッドをそのまま使用可（主キー制約の影響を受けないため）。 */

    @Delete("DELETE FROM salary_result WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);
}
