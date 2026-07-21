package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.EmployeeQualification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmployeeQualificationRepository extends BaseMapper<EmployeeQualification> {

    @Select("""
            SELECT * FROM employee_qualification
            WHERE employee_id = #{employeeId}
            ORDER BY acquisition_date
            """)
    List<EmployeeQualification> findByEmployeeIdOrderByAcquisitionDate(@Param("employeeId") Long employeeId);

    @Delete("DELETE FROM employee_qualification WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);
}
