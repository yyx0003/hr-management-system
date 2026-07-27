package com.example.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Position;

@Mapper
public interface PositionRepository
        extends BaseMapper<Position> {

    /** 対象役職の対象日付時点で有効なレコードを取得 */
    @Select("""
            SELECT *
            FROM "position"
            WHERE position_id = #{positionId}
            AND start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            """)
    Position findEffectiveAt(
            @Param("positionId") Long positionId,
            @Param("targetDate") LocalDate targetDate);

    /** 対象日時点で有効な役職一覧取得 */
    @Select("""
            SELECT *
            FROM "position"
            WHERE start_date <= #{targetDate}
            AND (end_date IS NULL OR end_date >= #{targetDate})
            ORDER BY position_id
            """)
    List<Position> findAllEffectiveAt(
            @Param("targetDate") LocalDate targetDate);

    /** 最新履歴取得 */
    @Select("""
            SELECT *
            FROM "position"
            WHERE position_id = #{positionId}
            ORDER BY start_date DESC
            LIMIT 1
            """)
    Position findLatestByPositionId(
            @Param("positionId") Long positionId);

    /** 終了日更新 */
    @Update("""
            UPDATE "position"
            SET end_date = #{endDate}
            WHERE position_id = #{positionId}
            AND start_date = #{startDate}
            """)
    int updateEndDate(Position position);

    /** 最大ID取得 */
    @Select("""
            SELECT MAX(position_id)
            FROM "position"
            """)
    Long findMaxId();

    /** 役職履歴削除 */
    @Delete("""
            DELETE FROM "position"
            WHERE position_id = #{positionId}
            AND start_date = #{startDate}
            """)
    int deletePosition(Position position);
}