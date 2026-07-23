package com.example.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.example.backend.entity.SkillGrade;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@Sql("/sql/skillgrade-test-data.sql")
class SkillGradeRepositoryTest {

    @Autowired
    private SkillGradeRepository skillGradeRepository;

    @Test
    @DisplayName("指定日時点で有効な職能資格を取得できる")
    void findEffectiveAtTest1() {

        LocalDate targetDate = LocalDate.of(2026, 5, 31);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                1,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getSkillGrade())
                .isEqualTo(1);
        assertThat(result.getAllowance())
                .isEqualTo(200000L);
    }

    @Test
    @DisplayName("開始日前は取得できない")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 3, 31);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                1,
                targetDate);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("履歴切替前は切替前の職能資格を取得する")
    void findEffectiveAtTest3() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                2,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(220000L);
    }

    @Test
    @DisplayName("履歴切替後は切替後の職能資格を取得する")
    void findEffectiveAtTest4() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                2,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(230000L);
    }

    @Test
    @DisplayName("3履歴ある職能資格の中間履歴を取得できる")
    void findEffectiveAtTest5() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                4,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(270000L);
    }

    @Test
    @DisplayName("3履歴ある職能資格の最新履歴を取得できる")
    void findEffectiveAtTest6() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                4,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(280000L);
    }

    @Test
    @DisplayName("存在しない等級の場合はnullを返す")
    void findEffectiveAtTest7() {

        SkillGrade result = skillGradeRepository.findEffectiveAt(
                99,
                LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("対象日時点で有効な職能資格一覧を取得できる")
    void findAllEffectiveAtTest1() {

        List<SkillGrade> skillGrades = skillGradeRepository.findAllEffectiveAt(
                LocalDate.of(2026, 7, 1));

        assertThat(skillGrades)
                .hasSize(10);

        assertThat(skillGrades)
                .extracting(SkillGrade::getSkillGrade)
                .containsExactly(
                        1, 2, 3, 4, 5,
                        6, 7, 8, 9, 10);
    }

    @Test
    @DisplayName("対象日時点で等級4の最新手当額を取得できる")
    void findAllEffectiveAtTest2() {

        List<SkillGrade> skillGrades = skillGradeRepository.findAllEffectiveAt(
                LocalDate.of(2026, 7, 1));

        SkillGrade grade4 = skillGrades.stream()
                .filter(g -> g.getSkillGrade() == 4)
                .findFirst()
                .orElseThrow();

        assertThat(grade4.getAllowance())
                .isEqualTo(280000L);
    }
}