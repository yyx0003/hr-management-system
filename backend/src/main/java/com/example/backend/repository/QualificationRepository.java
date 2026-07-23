package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Qualification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface QualificationRepository extends BaseMapper<Qualification> {

    /**
     * 対象資格の対象年月末日時点で有効な手当額・高度資格区分を取得する。
     * マスタ自体の手当額が改定される可能性があるため、現在値ではなく対象年月時点の値を参照する。
     */
    @Select("""
            SELECT * FROM qualification
            WHERE qualification_id = #{qualificationId}
            AND start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    Qualification findEffectiveAt(@Param("qualificationId") Long qualificationId,
                                   @Param("targetDate") LocalDate targetDate);

     /** 対象年月末日時点で有効な資格リストを取得する。*/
    @Select("""
            SELECT * FROM qualification
            WHERE start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    List<Qualification> findAllEffectiveAt(@Param("targetDate") LocalDate targetDate);
}
