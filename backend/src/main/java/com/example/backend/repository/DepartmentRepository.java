package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DepartmentRepository extends BaseMapper<Department> {

    /** 対象部署の対象日付時点で有効なレコードを取得する（部署名は改定される可能性があるため）。 */
    @Select("""
            SELECT * FROM department
            WHERE department_id = #{departmentId}
            AND start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    Department findEffectiveAt(@Param("departmentId") Long departmentId,
                                @Param("targetDate") LocalDate targetDate);
    
    /** 対象年月時点で有効なレコードを取得する */
    @Select("""
            SELECT * FROM department        
            WHERE start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            ORDER BY department_id
        """)
    List<Department> findAllEffectiveAt(@Param("targetDate") LocalDate targetDate);
}
