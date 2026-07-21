package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Attendance;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AttendanceRepository extends BaseMapper<Attendance> {

    @Select("""
            SELECT * FROM attendance
            WHERE employee_id = #{employeeId}
            AND work_date BETWEEN #{targetMonthStart} AND #{targetMonthEnd}
            ORDER BY work_date
            """)
    List<Attendance> findByEmployeeIdAndTargetMonth(@Param("employeeId") Long employeeId,
                                                      @Param("targetMonthStart") LocalDate targetMonthStart,
                                                      @Param("targetMonthEnd") LocalDate targetMonthEnd);

    @Delete("DELETE FROM attendance WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);
}
