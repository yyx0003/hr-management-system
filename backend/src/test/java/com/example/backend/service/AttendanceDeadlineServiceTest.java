package com.example.backend.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;

class AttendanceDeadlineServiceTest {

    private static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Tokyo");

    @Test
    void deadlineDayIsEditable() {

        AttendanceDeadlineService service =
                createService(
                        LocalDate.of(2026, 8, 5));

        assertDoesNotThrow(
                () -> service.validateEditable(
                        YearMonth.of(2026, 7)));
    }

    @Test
    void dayAfterDeadlineIsNotEditable() {

        MessageService messageService =
                org.mockito.Mockito.mock(
                        MessageService.class);

        when(messageService.getMessage(
                "error.attendance.edit.deadline.exceeded"))
                .thenReturn(
                        "対象年月の入力期限（翌月5日）を過ぎているため、変更できません。");

        AttendanceDeadlineService service =
                new AttendanceDeadlineService(
                        fixedClock(LocalDate.of(2026, 8, 6)),
                        messageService);

        assertThrows(
                BusinessException.class,
                () -> service.validateEditable(
                        YearMonth.of(2026, 7)));
    }

    @Test
    void futureMonthIsEditable() {

        AttendanceDeadlineService service =
                createService(
                        LocalDate.of(2026, 7, 22));

        assertTrue(
                service.isEditable(
                        YearMonth.of(2026, 8)));
    }

    @Test
    void expiredMonthIsNotEditable() {

        AttendanceDeadlineService service =
                createService(
                        LocalDate.of(2026, 8, 6));

        assertFalse(
                service.isEditable(
                        YearMonth.of(2026, 7)));
    }

    @Test
    void workDateCanBeValidated() {

        AttendanceDeadlineService service =
                createService(
                        LocalDate.of(2026, 8, 5));

        assertDoesNotThrow(
                () -> service.validateEditable(
                        LocalDate.of(2026, 7, 15)));
    }

    @Test
    void nullYearMonthIsNotEditable() {

        AttendanceDeadlineService service =
                createService(
                        LocalDate.of(2026, 7, 22));

        assertFalse(service.isEditable(null));
    }

    private AttendanceDeadlineService createService(
            LocalDate currentDate) {

        MessageService messageService =
                org.mockito.Mockito.mock(
                        MessageService.class);

        return new AttendanceDeadlineService(
                fixedClock(currentDate),
                messageService);
    }

    private Clock fixedClock(LocalDate date) {

        Instant instant =
                date.atStartOfDay(ZONE_ID)
                        .toInstant();

        return Clock.fixed(
                instant,
                ZONE_ID);
    }
}