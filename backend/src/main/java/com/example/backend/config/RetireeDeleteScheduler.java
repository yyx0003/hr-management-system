package com.example.backend.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.backend.controller.SalaryConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 退職者削除バッチの起動契機。
 * 毎日3:00に自動起動する（手動実行用のAPIは用意しない）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetireeDeleteScheduler {

    private final JobLauncher jobLauncher;
    private final Job retireeDeleteJob;

    @Scheduled(cron = "0 0 3 * * *")
    public void run() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(SalaryConstants.RETENTION_MONTHS);
        try {
            var jobParameters = new JobParametersBuilder()
                    .addString("cutoffDate", cutoffDate.toString())
                    .addLocalDateTime("executedAt", LocalDateTime.now())
                    .toJobParameters();
            jobLauncher.run(retireeDeleteJob, jobParameters);
        } catch (Exception e) {
            log.error("退職者削除バッチの起動に失敗しました。", e);
        }
    }
}