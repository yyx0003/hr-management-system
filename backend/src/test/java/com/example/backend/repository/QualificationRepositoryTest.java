package com.example.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.entity.Qualification;

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
    @DisplayName("指定日時点で有効な資格を取得できる")
    void findEffectiveAtTest1() {

        LocalDate targetDate = LocalDate.of(2026, 5, 31);

        Qualification result = qualificationRepository.findEffectiveAt(
                1L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationId())
                .isEqualTo(1L);
        assertThat(result.getQualificationName())
                .isEqualTo("基本情報技術者試験");
        assertThat(result.getQualificationAllowance())
                .isEqualTo(3000L);
    }

    @Test
    @DisplayName("開始日前は取得できない")
    void findEffectiveAtTest2() {

        LocalDate targetDate = LocalDate.of(2026, 3, 31);

        Qualification result = qualificationRepository.findEffectiveAt(
                1L,
                targetDate);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("履歴切替前は切替前の資格情報を取得する")
    void findEffectiveAtTest3() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        Qualification result = qualificationRepository.findEffectiveAt(
                2L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationAllowance())
                .isEqualTo(5000L);
    }

    @Test
    @DisplayName("履歴切替後は切替後の資格情報を取得する")
    void findEffectiveAtTest4() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        Qualification result = qualificationRepository.findEffectiveAt(
                2L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationAllowance())
                .isEqualTo(6000L);
    }

    @Test
    @DisplayName("3履歴ある資格の中間履歴を取得できる")
    void findEffectiveAtTest5() {

        LocalDate targetDate = LocalDate.of(2026, 6, 30);

        Qualification result = qualificationRepository.findEffectiveAt(
                4L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationAllowance())
                .isEqualTo(35000L);
    }

    @Test
    @DisplayName("3履歴ある資格の最新履歴を取得できる")
    void findEffectiveAtTest6() {

        LocalDate targetDate = LocalDate.of(2026, 7, 1);

        Qualification result = qualificationRepository.findEffectiveAt(
                4L,
                targetDate);

        assertThat(result).isNotNull();
        assertThat(result.getQualificationAllowance())
                .isEqualTo(40000L);
    }

    @Test
    @DisplayName("存在しない資格IDの場合はnull")
    void findEffectiveAtTest7() {

        Qualification result = qualificationRepository.findEffectiveAt(
                999L,
                LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("高度資格フラグを取得できる")
    void findEffectiveAtTest8() {

        Qualification result = qualificationRepository.findEffectiveAt(
                3L,
                LocalDate.of(2026, 7, 1));

        assertThat(result).isNotNull();
        assertThat(result.getIsAdvance()).isTrue();
    }

    @Test
    @DisplayName("対象日時点で有効な資格一覧を取得できる")
    void findAllEffectiveAtTest1() {

        List<Qualification> qualifications = qualificationRepository.findAllEffectiveAt(
                LocalDate.of(2026, 7, 1));

        assertThat(qualifications)
                .hasSize(7);

        assertThat(qualifications)
                .extracting(Qualification::getQualificationName)
                .containsExactlyInAnyOrder(
                        "基本情報技術者試験",
                        "応用情報技術者",
                        "システムアーキテクト",
                        "プロジェクトマネージャ",
                        "ネットワークスペシャリスト",
                        "データベーススペシャリスト",
                        "エンベデッドシステムスペシャリスト");
        
        assertThat(qualifications)
                .extracting(Qualification::getQualificationAllowance)
                .containsExactlyInAnyOrder(
                        3000L,
                        6000L,
                        30000L,
                        35000L,
                        40000L,
                        30000L,
                        30000L);
    }

    @Test
    @DisplayName("対象日前日で有効な資格一覧を取得できる")
    void findAllEffectiveAtTest2() {

        List<Qualification> qualifications = qualificationRepository.findAllEffectiveAt(
                LocalDate.of(2026, 6, 30));

        assertThat(qualifications)
                .hasSize(7);

        assertThat(qualifications)
                .extracting(Qualification::getQualificationName)
                .containsExactlyInAnyOrder(
                        "基本情報技術者試験",
                        "応用情報技術者",
                        "システムアーキテクト",
                        "プロジェクトマネージャ",
                        "ネットワークスペシャリスト",
                        "データベーススペシャリスト",
                        "エンベデッドシステムスペシャリスト");

        assertThat(qualifications)
                .extracting(Qualification::getQualificationAllowance)
                .containsExactlyInAnyOrder(
                        3000L,
                        5000L,
                        30000L,
                        35000L,
                        35000L,
                        30000L,
                        30000L);
    }
}