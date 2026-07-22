package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonthlyDateServiceTest {

    private MonthlyDateService monthlyDateService;

    @BeforeEach
    void setUp() {
        monthlyDateService =
                new MonthlyDateService();
    }

    @Test
    void createsAllDatesForThirtyOneDayMonth() {

        List<LocalDate> result =
                monthlyDateService.createDates(
                        YearMonth.of(2026, 7));

        assertEquals(31, result.size());
        assertEquals(
                LocalDate.of(2026, 7, 1),
                result.get(0));
        assertEquals(
                LocalDate.of(2026, 7, 31),
                result.get(30));
    }

    @Test
    void createsAllDatesForThirtyDayMonth() {

        List<LocalDate> result =
                monthlyDateService.createDates(
                        YearMonth.of(2026, 6));

        assertEquals(30, result.size());
        assertEquals(
                LocalDate.of(2026, 6, 30),
                result.get(29));
    }

    @Test
    void createsTwentyEightDatesForNormalFebruary() {

        List<LocalDate> result =
                monthlyDateService.createDates(
                        YearMonth.of(2026, 2));

        assertEquals(28, result.size());
        assertEquals(
                LocalDate.of(2026, 2, 28),
                result.get(27));
    }

    @Test
    void createsTwentyNineDatesForLeapYearFebruary() {

        List<LocalDate> result =
                monthlyDateService.createDates(
                        YearMonth.of(2028, 2));

        assertEquals(29, result.size());
        assertEquals(
                LocalDate.of(2028, 2, 29),
                result.get(28));
    }

    @Test
    void nullYearMonthThrowsException() {

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> monthlyDateService.createDates(null));

        assertEquals(
                "targetYearMonth must not be null",
                exception.getMessage());
    }
}