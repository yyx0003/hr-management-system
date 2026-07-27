package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Department;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

        /** 対象年月時点で有効なレコードを取得する. */
        @Select("""
                            SELECT * FROM department
                            WHERE start_date <= #{targetDate}
                            AND (end_date IS NULL OR end_date >= #{targetDate})
                            ORDER BY department_id
                        """)
        List<Department> findAllEffectiveAt(@Param("targetDate") LocalDate targetDate);

        /** 対象部署で最も開始日が先のレコードを取得する. */
        @Select("""
                            SELECT * FROM department
                            WHERE department_id = #{departmentId}
                            ORDER BY start_date DESC
                            LIMIT 1
                        """)
        Department findLatestByDepartmentId(@Param("departmentId") Long departmentId);

        /** 対象部署で最も開始日が先のレコードに対して、終了日を設定 */
        @Update("""
                            UPDATE department
                            SET end_date = #{endDate}
                            WHERE department_id = #{departmentId}
                            AND start_date = #{startDate}
                        """)
        int updateEndDate(Department department);

        /** 部署IDの最大値を取得. */
        @Select("""
                            SELECT MAX(department_id)
                            FROM department
                        """)
        Long findMaxId();

        /** 対象年月、対象部署の部署履歴を削除 */
        @Delete("""
                            DELETE FROM department
                            WHERE department_id = #{departmentId}
                            AND start_date = #{startDate}
                        """)
        int deleteDepartment(Department department);
}
