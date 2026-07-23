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

import com.example.backend.entity.Holiday;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@Sql("/sql/holiday-test-data.sql")
@ActiveProfiles("test")
@Transactional
class HolidayRepositoryTest {

    @Autowired
    private HolidayRepository holidayRepository;

    @Test
    @DisplayName("単一の祝日を取得できる")
    void findByDateRangeTest1() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 1));

        assertThat(holidays).hasSize(1);

        Holiday holiday = holidays.get(0);

        assertThat(holiday.getHolidayName())
                .isEqualTo("元日");

        assertThat(holiday.getHolidayType())
                .isEqualTo("HOLIDAY");
    }

    @Test
    @DisplayName("ゴールデンウィーク期間の祝日を取得できる")
    void findByDateRangeTest2() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 31));

        assertThat(holidays).hasSize(4);

        assertThat(holidays)
                .extracting(Holiday::getHolidayName)
                .containsExactlyInAnyOrder(
                        "憲法記念日",
                        "みどりの日",
                        "こどもの日",
                        "振替休日");
    }

    @Test
    @DisplayName("夏季休暇を取得できる")
    void findByDateRangeTest3() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 8, 1),
                        LocalDate.of(2026, 8, 31));

        assertThat(holidays)
                .extracting(Holiday::getHolidayName)
                .containsExactlyInAnyOrder(
                        "山の日",
                        "夏季休暇",
                        "夏季休暇");

        assertThat(holidays)
                .extracting(Holiday::getHolidayType)
                .contains("SUMMER");
    }

    @Test
    @DisplayName("冬期休暇を取得できる")
    void findByDateRangeTest4() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 12, 1),
                        LocalDate.of(2026, 12, 31));

        assertThat(holidays).hasSize(3);

        assertThat(holidays)
                .extracting(Holiday::getHolidayType)
                .containsOnly("WINTER");
    }

    @Test
    @DisplayName("祝日が存在しない期間の場合は空リストを返す")
    void findByDateRangeTest5() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30));

        assertThat(holidays).isEmpty();
    }

    @Test
    @DisplayName("年間の休日一覧を取得できる")
    void findByDateRangeTest6() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31));

        assertThat(holidays).hasSize(23);
    }

    @Test
    @DisplayName("シルバーウィーク期間の休日を取得できる")
    void findByDateRangeTest7() {

        List<Holiday> holidays =
                holidayRepository.findByDateRange(
                        LocalDate.of(2026, 9, 21),
                        LocalDate.of(2026, 9, 23));

        assertThat(holidays).hasSize(3);

        assertThat(holidays)
                .extracting(Holiday::getHolidayName)
                .containsExactly(
                        "敬老の日",
                        "国民の休日",
                        "秋分の日");
    }
}