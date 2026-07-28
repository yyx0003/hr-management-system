package com.example.backend.repository;

import com.example.backend.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@Sql("/sql/position-test-data.sql")
@ActiveProfiles("test")
@Transactional
class PositionRepositoryTest {

    @Autowired
    private PositionRepository positionRepository;

    @Test
    @DisplayName("レコードが1件だけの場合（開始日当日）")
    void findEffectiveAtTest1() {

        Position result =
                positionRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result).isNotNull();
        assertThat(result.getPositionName())
                .isEqualTo("主任");
    }

    @Test
    @DisplayName("レコードが1件だけの場合（開始日翌日）")
    void findEffectiveAtTest2() {

        Position result =
                positionRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 2));

        assertThat(result).isNotNull();
        assertThat(result.getPositionName())
                .isEqualTo("主任");
    }

    @Test
    @DisplayName("開始日前日は取得できない")
    void findEffectiveAtTest3() {

        Position result =
                positionRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("1つ目の終了日当日")
    void findEffectiveAtTest4() {

        Position result =
                positionRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 4, 30));

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(20000L);
    }

    @Test
    @DisplayName("2つ目の開始日当日")
    void findEffectiveAtTest5() {

        Position result =
                positionRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(25000L);
    }

    @Test
    @DisplayName("2つ目の終了日当日")
    void findEffectiveAtTest6() {

        Position result =
                positionRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 31));

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(25000L);
    }

    @Test
    @DisplayName("3つ目の開始日当日")
    void findEffectiveAtTest7() {

        Position result =
                positionRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 6, 1));

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("終了日当日は取得できる")
    void findEffectiveAtTest8() {

        Position result =
                positionRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 6, 30));

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(60000L);
    }

    @Test
    @DisplayName("終了日翌日は取得できない")
    void findEffectiveAtTest9() {

        Position result =
                positionRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("どのレコードよりも過去の日付")
    void findAllEffectiveAtTest1() {

        List<Position> result =
                positionRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("変更日の前日")
    void findAllEffectiveAtTest2() {

        List<Position> result =
                positionRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 4, 30));

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("変更日の当日")
    void findAllEffectiveAtTest3() {

        List<Position> result =
                positionRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 5, 1));

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴1件）")
    void findLatestByPositionIdTest1() {

        Position result =
                positionRepository.findLatestByPositionId(
                        1L);

        assertThat(result).isNotNull();
        assertThat(result.getPositionName())
                .isEqualTo("主任");
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴複数）")
    void findLatestByPositionIdTest2() {

        Position result =
                positionRepository.findLatestByPositionId(
                        2L);

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("最新履歴を取得できる（全履歴終了済み）")
    void findLatestByPositionIdTest3() {

        Position result =
                positionRepository.findLatestByPositionId(
                        3L);

        assertThat(result).isNotNull();
        assertThat(result.getPositionAllowance())
                .isEqualTo(60000L);
    }

    @Test
    @DisplayName("存在しないIDの場合はnull")
    void findLatestByPositionIdTest4() {

        Position result =
                positionRepository.findLatestByPositionId(
                        999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("終了日更新（履歴1件）")
    void updateEndDateTest1() {

        Position position =
                positionRepository.findLatestByPositionId(1L);

        position.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                positionRepository.updateEndDate(position);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("終了日更新（履歴複数）")
    void updateEndDateTest2() {

        Position position =
                positionRepository.findLatestByPositionId(2L);

        position.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                positionRepository.updateEndDate(position);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("存在しないレコードの更新は0件")
    void updateEndDateTest3() {

        Position position = new Position();

        position.setPositionId(999L);
        position.setStartDate(
                LocalDate.of(2026, 1, 1));
        position.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                positionRepository.updateEndDate(position);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("役職IDの最大値を取得できる")
    void findMaxIdTest1() {

        Long result =
                positionRepository.findMaxId();

        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("役職履歴削除（履歴1件）")
    void deletePositionTest1() {

        Position position =
                positionRepository.findLatestByPositionId(1L);

        int result =
                positionRepository.deletePosition(position);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("役職履歴削除（履歴複数の最新レコード）")
    void deletePositionTest2() {

        Position position =
                positionRepository.findLatestByPositionId(2L);

        int result =
                positionRepository.deletePosition(position);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("役職履歴削除（全履歴終了済みの最新レコード）")
    void deletePositionTest3() {

        Position position =
                positionRepository.findLatestByPositionId(3L);

        int result =
                positionRepository.deletePosition(position);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("存在しない役職の削除は0件")
    void deletePositionTest4() {

        Position position = new Position();

        position.setPositionId(999L);
        position.setStartDate(
                LocalDate.of(2026, 1, 1));

        int result =
                positionRepository.deletePosition(position);

        assertThat(result).isEqualTo(0);
    }
}