package com.example.backend.repository;

import com.example.backend.entity.Qualification;
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
@Sql("/sql/qualification-test-data.sql")
@ActiveProfiles("test")
@Transactional
class QualificationRepositoryTest {

    @Autowired
    private QualificationRepository qualificationRepository;

    @Test
    @DisplayName("レコードが1件だけの場合（開始日当日）")
    void findEffectiveAtTest1() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result).isNotNull();
        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者");
    }

    @Test
    @DisplayName("レコードが1件だけの場合（開始日翌日）")
    void findEffectiveAtTest2() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 2));

        assertThat(result).isNotNull();
        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者");
    }

    @Test
    @DisplayName("開始日前日は取得できない")
    void findEffectiveAtTest3() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("1つ目の終了日当日")
    void findEffectiveAtTest4() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 4, 30));

        assertThat(result.getQualificationName())
                .isEqualTo("応用情報技術者");
    }

    @Test
    @DisplayName("2つ目の開始日当日")
    void findEffectiveAtTest5() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result.getQualificationName())
                .isEqualTo("応用情報技術者");
    }

    @Test
    @DisplayName("2つ目の終了日当日")
    void findEffectiveAtTest6() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 31));

        assertThat(result.getQualificationName())
                .isEqualTo("応用情報技術者");
    }

    @Test
    @DisplayName("3つ目の開始日当日")
    void findEffectiveAtTest7() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 6, 1));

        assertThat(result.getQualificationAllowance())
                .isEqualTo(15000L);
    }

    @Test
    @DisplayName("終了日当日は取得できる")
    void findEffectiveAtTest8() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 6, 30));

        assertThat(result.getQualificationAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("終了日翌日は取得できない")
    void findEffectiveAtTest9() {

        Qualification result =
                qualificationRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("どのレコードよりも過去の日付")
    void findAllEffectiveAtTest1() {

        List<Qualification> result =
                qualificationRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("変更日の前日")
    void findAllEffectiveAtTest2() {

        List<Qualification> result =
                qualificationRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 4, 30));

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("変更日の当日")
    void findAllEffectiveAtTest3() {

        List<Qualification> result =
                qualificationRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 5, 1));

        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴1件）")
    void findLatestByQualificationIdTest1() {

        Qualification result =
                qualificationRepository
                        .findLatestByQualificationId(1L);

        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者");
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴複数）")
    void findLatestByQualificationIdTest2() {

        Qualification result =
                qualificationRepository
                        .findLatestByQualificationId(2L);

        assertThat(result.getQualificationAllowance())
                .isEqualTo(15000L);
    }

    @Test
    @DisplayName("最新履歴を取得できる（全履歴終了済み）")
    void findLatestByQualificationIdTest3() {

        Qualification result =
                qualificationRepository
                        .findLatestByQualificationId(3L);

        assertThat(result.getQualificationAllowance())
                .isEqualTo(30000L);
    }

    @Test
    @DisplayName("存在しないIDの場合はnull")
    void findLatestByQualificationIdTest4() {

        Qualification result =
                qualificationRepository
                        .findLatestByQualificationId(999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("終了日更新（履歴1件）")
    void updateEndDateTest1() {

        Qualification qualification =
                qualificationRepository
                        .findLatestByQualificationId(1L);

        qualification.setEndDate(
                LocalDate.of(2026, 12, 31));

        assertThat(
                qualificationRepository.updateEndDate(
                        qualification))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("終了日更新（履歴複数）")
    void updateEndDateTest2() {

        Qualification qualification =
                qualificationRepository
                        .findLatestByQualificationId(2L);

        qualification.setEndDate(
                LocalDate.of(2026, 12, 31));

        assertThat(
                qualificationRepository.updateEndDate(
                        qualification))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("存在しないレコードの更新は0件")
    void updateEndDateTest3() {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(999L);
        qualification.setStartDate(
                LocalDate.of(2026, 1, 1));
        qualification.setEndDate(
                LocalDate.of(2026, 12, 31));

        assertThat(
                qualificationRepository.updateEndDate(
                        qualification))
                .isEqualTo(0);
    }

    @Test
    @DisplayName("資格IDの最大値を取得できる")
    void findMaxIdTest1() {

        assertThat(
                qualificationRepository.findMaxId())
                .isEqualTo(3L);
    }

    @Test
    @DisplayName("資格履歴削除（履歴1件）")
    void deleteQualificationTest1() {

        Qualification qualification =
                qualificationRepository
                        .findLatestByQualificationId(1L);

        assertThat(
                qualificationRepository
                        .deleteQualification(
                                qualification))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("資格履歴削除（履歴複数の最新レコード）")
    void deleteQualificationTest2() {

        Qualification qualification =
                qualificationRepository
                        .findLatestByQualificationId(2L);

        assertThat(
                qualificationRepository
                        .deleteQualification(
                                qualification))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("資格履歴削除（全履歴終了済みの最新レコード）")
    void deleteQualificationTest3() {

        Qualification qualification =
                qualificationRepository
                        .findLatestByQualificationId(3L);

        assertThat(
                qualificationRepository
                        .deleteQualification(
                                qualification))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("存在しない資格の削除は0件")
    void deleteQualificationTest4() {

        Qualification qualification =
                new Qualification();

        qualification.setQualificationId(999L);
        qualification.setStartDate(
                LocalDate.of(2026, 1, 1));

        assertThat(
                qualificationRepository
                        .deleteQualification(
                                qualification))
                .isEqualTo(0);
    }
}