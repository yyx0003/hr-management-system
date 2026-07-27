package com.example.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.SkillGrade;

@Mapper
public interface SkillGradeRepository
        extends BaseMapper<SkillGrade> {

    /** 対象等級の対象日付時点で有効なレコードを取得 */
    @Select("""
            SELECT *
            FROM skill_grade
            WHERE skill_grade = #{skillGrade}
            AND start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    SkillGrade findEffectiveAt(
            @Param("skillGrade") Integer skillGrade,
            @Param("targetDate") LocalDate targetDate);

    /** 対象日時点で有効な等級一覧取得 */
    @Select("""
            SELECT *
            FROM skill_grade
            WHERE start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            ORDER BY skill_grade
            """)
    List<SkillGrade> findAllEffectiveAt(
            @Param("targetDate") LocalDate targetDate);

    /** 最新履歴取得 */
    @Select("""
            SELECT *
            FROM skill_grade
            WHERE skill_grade = #{skillGrade}
            ORDER BY start_date DESC
            LIMIT 1
            """)
    SkillGrade findLatestBySkillGrade(
            @Param("skillGrade") Integer skillGrade);

    /** 終了日更新 */
    @Update("""
            UPDATE skill_grade
            SET end_date = #{endDate}
            WHERE skill_grade = #{skillGrade}
            AND start_date = #{startDate}
            """)
    int updateEndDate(SkillGrade skillGrade);

    /** 等級履歴削除 */
    @Delete("""
            DELETE FROM skill_grade
            WHERE skill_grade = #{skillGrade}
            AND start_date = #{startDate}
            """)
    int deleteSkillGrade(SkillGrade skillGrade);
}