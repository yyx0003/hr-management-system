package com.example.backend.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

/**
 * 対象年月の日付一覧を生成するサービス。
 */
@Service
public class MonthlyDateService {

    /**
     * 対象年月の1日から月末までの日付一覧を返す。
     *
     * @param targetYearMonth 対象年月
     * @return 対象月の日付一覧
     */
    public List<LocalDate> createDates(
            YearMonth targetYearMonth) {

        if (targetYearMonth == null) {
            throw new IllegalArgumentException(
                    "targetYearMonth must not be null");
        }

        return IntStream
                .rangeClosed(
                        1,
                        targetYearMonth.lengthOfMonth())
                .mapToObj(targetYearMonth::atDay)
                .toList();
    }
}