package com.example.backend.repository;

import com.example.backend.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@Sql("/sql/position-test-data.sql")
class PositionRepositoryTest {

    @Autowired
    private PositionRepository positionRepository;

    @Test
    @DisplayName("指定日時点で有効な役職を取得できる（開始日から1か月経過）")
    void findEffectiveAtTest1() {

        LocalDate targetDate = LocalDate.of(2026, 5, 31);

        Position result = positionRepository.findEffectiveAt(
                1L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(1L);
        assertThat(result.getPositionName()).isEqualTo("部長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(50000L);
    }

    @Test
    @DisplayName("指定日時点で有効な役職を取得できる（開始日と同一）")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 4, 1);

        Position result = positionRepository.findEffectiveAt(
                2L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(2L);
        assertThat(result.getPositionName()).isEqualTo("副部長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(30000L);
    }

    @Test
    @DisplayName("開始日の前日は取得できない")
    void findEffectiveAtTest3() {

        LocalDate targetDate = LocalDate.of(2026, 3, 31);

        Position result = positionRepository.findEffectiveAt(
                1L,
                targetDate);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("履歴切替日の前日は切替前の情報を取得する")
    void findEffectiveAtTest4() {

        LocalDate targetDate = LocalDate.of(2026, 4, 30);

        Position result = positionRepository.findEffectiveAt(
                2L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(2L);
        assertThat(result.getPositionName()).isEqualTo("副部長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(30000L);
    }

    @Test
    @DisplayName("履歴切替日当日は切替後の情報を取得する")
    void findEffectiveAtTest5() {

        LocalDate targetDate = LocalDate.of(2026, 5, 1);

        Position result = positionRepository.findEffectiveAt(
                2L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(2L);
        assertThat(result.getPositionName()).isEqualTo("副部長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(35000L);
    }

    @Test
    @DisplayName("役職情報が3つ存在する場合に対象日時点の役職を取得できる")
    void findEffectiveAtTest6() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        Position result = positionRepository.findEffectiveAt(
                3L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(3L);
        assertThat(result.getPositionName()).isEqualTo("課長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(25000L);
    }

    @Test
    @DisplayName("3世代目の履歴を取得できる")
    void findEffectiveAtTest7() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        Position result = positionRepository.findEffectiveAt(
                3L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getPositionId()).isEqualTo(3L);
        assertThat(result.getPositionName()).isEqualTo("課長");
        assertThat(result.getPositionAllowance()).isEqualByComparingTo(30000L);
    }

    @Test
    @DisplayName("存在しない役職IDが指定された場合はnullを返す")
    void findEffectiveAtTest8() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        Position result = positionRepository.findEffectiveAt(
                999L,
                targetDate);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("履歴切替前の有効な役職一覧を取得できる")
    void findAllEffectiveAtTest1() {

        LocalDate targetDate = LocalDate.of(2026, 4, 30);

        List<Position> positions = positionRepository.findAllEffectiveAt(
                targetDate);

        assertThat(positions).hasSize(4);

        assertThat(positions)
                .extracting(Position::getPositionName)
                .containsExactlyInAnyOrder(
                        "部長",
                        "副部長",
                        "課長",
                        "係長");
    }

    @Test
    @DisplayName("履歴切替後の有効な役職一覧を取得できる")
    void findAllEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        List<Position> positions = positionRepository.findAllEffectiveAt(
                targetDate);

        assertThat(positions).hasSize(4);

        assertThat(positions)
                .extracting(Position::getPositionName)
                .containsExactlyInAnyOrder(
                        "部長",
                        "副部長",
                        "課長",
                        "課長代理");
    }
}