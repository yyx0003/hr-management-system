package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SkillGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;

@Mapper
public interface SkillGradeRepository extends BaseMapper<SkillGrade> {

    /** 対象等級の対象年月末日時点で有効な手当額を取得する。 */
    @Select("""
            SELECT * FROM skill_grade
            WHERE skill_grade = #{skillGrade}
            AND start_date <= #{targetMonthEnd}
            AND (end_date IS NULL OR end_date > #{targetMonthEnd})
            """)
    SkillGrade findEffectiveAt(@Param("skillGrade") Integer skillGrade,
                                @Param("targetMonthEnd") LocalDate targetMonthEnd);
}
