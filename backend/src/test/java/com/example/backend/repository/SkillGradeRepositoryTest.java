package com.example.backend.repository;

import com.example.backend.entity.SkillGrade;
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
@Sql("/sql/skillgrade-test-data.sql")
@ActiveProfiles("test")
@Transactional
class SkillGradeRepositoryTest {

    @Autowired
    private SkillGradeRepository skillGradeRepository;

    @Test
    @DisplayName("レコードが1件だけの場合（開始日当日）")
    void findEffectiveAtTest1() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        1,
                        LocalDate.of(2026, 4, 1));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(10000L);
    }

    @Test
    @DisplayName("レコードが1件だけの場合（開始日翌日）")
    void findEffectiveAtTest2() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        1,
                        LocalDate.of(2026, 4, 2));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(10000L);
    }

    @Test
    @DisplayName("開始日前日は取得できない")
    void findEffectiveAtTest3() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        1,
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("1つ目の終了日当日")
    void findEffectiveAtTest4() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        2,
                        LocalDate.of(2026, 4, 30));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(20000L);
    }

    @Test
    @DisplayName("2つ目の開始日当日")
    void findEffectiveAtTest5() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        2,
                        LocalDate.of(2026, 5, 1));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(25000L);
    }

    @Test
    @DisplayName("2つ目の終了日当日")
    void findEffectiveAtTest6() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        2,
                        LocalDate.of(2026, 5, 31));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(25000L);
    }

    @Test
    @DisplayName("3つ目の開始日当日")
    void findEffectiveAtTest7() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        2,
                        LocalDate.of(2026, 6, 1));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("終了日当日は取得できる")
    void findEffectiveAtTest8() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        3,
                        LocalDate.of(2026, 6, 30));

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(40000L);
    }

    @Test
    @DisplayName("終了日翌日は取得できない")
    void findEffectiveAtTest9() {

        SkillGrade result =
                skillGradeRepository.findEffectiveAt(
                        3,
                        LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("どのレコードよりも過去の日付")
    void findAllEffectiveAtTest1() {

        List<SkillGrade> result =
                skillGradeRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("変更日の前日")
    void findAllEffectiveAtTest2() {

        List<SkillGrade> result =
                skillGradeRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 4, 30));

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("変更日の当日")
    void findAllEffectiveAtTest3() {

        List<SkillGrade> result =
                skillGradeRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 5, 1));

        assertThat(result).hasSize(10);
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴1件）")
    void findLatestBySkillGradeTest1() {

        SkillGrade result =
                skillGradeRepository.findLatestBySkillGrade(1);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(10000L);
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴複数）")
    void findLatestBySkillGradeTest2() {

        SkillGrade result =
                skillGradeRepository.findLatestBySkillGrade(2);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("最新履歴を取得できる（全履歴終了済み）")
    void findLatestBySkillGradeTest3() {

        SkillGrade result =
                skillGradeRepository.findLatestBySkillGrade(3);

        assertThat(result).isNotNull();
        assertThat(result.getAllowance())
                .isEqualTo(40000L);
    }

    @Test
    @DisplayName("存在しないIDの場合はnull")
    void findLatestBySkillGradeTest4() {

        SkillGrade result =
                skillGradeRepository.findLatestBySkillGrade(999);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("終了日更新（履歴1件）")
    void updateEndDateTest1() {

        SkillGrade skillGrade =
                skillGradeRepository.findLatestBySkillGrade(1);

        skillGrade.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                skillGradeRepository.updateEndDate(skillGrade);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("終了日更新（履歴複数）")
    void updateEndDateTest2() {

        SkillGrade skillGrade =
                skillGradeRepository.findLatestBySkillGrade(2);

        skillGrade.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                skillGradeRepository.updateEndDate(skillGrade);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("存在しないレコードの更新は0件")
    void updateEndDateTest3() {

        SkillGrade skillGrade = new SkillGrade();

        skillGrade.setSkillGrade(999);
        skillGrade.setStartDate(
                LocalDate.of(2026, 1, 1));
        skillGrade.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                skillGradeRepository.updateEndDate(skillGrade);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("職能資格履歴削除（履歴1件）")
    void deleteSkillGradeTest1() {

        SkillGrade skillGrade =
                skillGradeRepository.findLatestBySkillGrade(1);

        int result =
                skillGradeRepository.deleteSkillGrade(skillGrade);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("職能資格履歴削除（履歴複数の最新レコード）")
    void deleteSkillGradeTest2() {

        SkillGrade skillGrade =
                skillGradeRepository.findLatestBySkillGrade(2);

        int result =
                skillGradeRepository.deleteSkillGrade(skillGrade);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("職能資格履歴削除（全履歴終了済みの最新レコード）")
    void deleteSkillGradeTest3() {

        SkillGrade skillGrade =
                skillGradeRepository.findLatestBySkillGrade(3);

        int result =
                skillGradeRepository.deleteSkillGrade(skillGrade);

        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("存在しない職能資格の削除は0件")
    void deleteSkillGradeTest4() {

        SkillGrade skillGrade = new SkillGrade();

        skillGrade.setSkillGrade(999);
        skillGrade.setStartDate(
                LocalDate.of(2026, 1, 1));

        int result =
                skillGradeRepository.deleteSkillGrade(skillGrade);

        assertThat(result).isEqualTo(0);
    }
}