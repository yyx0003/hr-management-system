package com.example.backend.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 【注意】テーブル名は"position"（ダブルクォート付き）で作成されているため、
 * カスタムSQL内でも同様にダブルクォートで囲む必要がある。
 */
@Mapper
public interface PositionRepository extends BaseMapper<Position> {

    /** 対象役職の対象年月末日時点で有効な手当額を取得する。 */
    @Select("""
            SELECT * FROM "position"
            WHERE position_id = #{positionId}
            AND start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    Position findEffectiveAt(@Param("positionId") Long positionId,
                              @Param("targetDate") LocalDate targetDate);

    /** 対象年月時点で有効な役職リストを取得する。 */
    @Select("""
            SELECT * FROM "position"
            WHERE start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    List<Position> findAllEffectiveAt(@Param("targetDate") LocalDate targetDate);
}
