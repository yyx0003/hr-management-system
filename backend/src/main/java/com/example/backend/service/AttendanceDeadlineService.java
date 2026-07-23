package com.example.backend.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.stereotype.Service;

import com.example.backend.common.MessageService;
import com.example.backend.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * 勤怠の入力期限を確認するサービス。
 */
@Service
@RequiredArgsConstructor
public class AttendanceDeadlineService {

    /** 入力期限の日 */
    private static final int DEADLINE_DAY = 5;

    /** 現在日時取得用 */
    private final Clock clock;

    /** メッセージ取得用 */
    private final MessageService messageService;

    /**
     * 対象年月の勤怠が変更可能か確認する。
     *
     * 対象月の翌月5日までは変更可能とし、
     * 翌月6日以降は変更不可とする。
     *
     * @param targetYearMonth 対象年月
     */
    public void validateEditable(YearMonth targetYearMonth) {

        if (targetYearMonth == null) {
            throw new IllegalArgumentException(
                    "targetYearMonth must not be null");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate deadline =
                targetYearMonth
                        .plusMonths(1)
                        .atDay(DEADLINE_DAY);

        if (today.isAfter(deadline)) {
            throw new BusinessException(
                    messageService.getMessage(
                            "error.attendance.edit.deadline.exceeded"));
        }
    }

    /**
     * 勤怠日をもとに入力期限を確認する。
     *
     * @param workDate 勤怠日
     */
    public void validateEditable(LocalDate workDate) {

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "workDate must not be null");
        }

        validateEditable(YearMonth.from(workDate));
    }

    /**
     * 対象年月が変更可能か返す。
     *
     * @param targetYearMonth 対象年月
     * @return 変更可能な場合はtrue
     */
    public boolean isEditable(YearMonth targetYearMonth) {

        if (targetYearMonth == null) {
            return false;
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate deadline =
                targetYearMonth
                        .plusMonths(1)
                        .atDay(DEADLINE_DAY);

        return !today.isAfter(deadline);
    }
}