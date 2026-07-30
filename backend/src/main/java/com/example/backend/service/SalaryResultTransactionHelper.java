package com.example.backend.service;

import java.time.YearMonth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.repository.SalaryResultRepository;

import lombok.RequiredArgsConstructor;

/**
 * salary_resultの削除・再計算を、呼び出し元のトランザクションから
 * 独立させて実行するためのヘルパー。
 * 勤怠保存トランザクションを給与計算の失敗で巻き込まないために使用する。
 */
@Service
@RequiredArgsConstructor
public class SalaryResultTransactionHelper {

    private final SalaryResultRepository salaryResultRepository;
    private final SalaryCalculationService salaryCalculationService;

    /** 対象社員・対象年月のsalary_resultを削除する（独立トランザクション）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteSalaryResultOnly(Long employeeId, YearMonth targetYearMonth) {
        salaryResultRepository.deleteByEmployeeIdAndTargetMonth(
                employeeId, targetYearMonth.getYear(), targetYearMonth.getMonthValue());
    }

    /** 対象社員1名分の給与計算を行う（独立トランザクション）。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void calculateOneInNewTransaction(Long employeeId, YearMonth targetYearMonth) {
        salaryCalculationService.calculateOne(employeeId, targetYearMonth);
    }
}