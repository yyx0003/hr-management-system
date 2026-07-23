package com.example.backend.batch.retiree;

import java.time.LocalDate;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.backend.service.EmployeeService;
import com.example.backend.service.RetireeDeleteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 退職者削除バッチのJob／Step／Reader／Writer／Listenerを1ファイルにまとめたもの
 * （各パーツが小さく、他機能から個別に参照されないため統合している）。
 *
 * チャンクサイズ1（社員1件ごとにトランザクションをコミット）で構成する。
 * 特定の社員の削除処理でエラーが発生した場合は、当該社員のみスキップし、
 * 他の社員の処理には影響を与えない（faultTolerant + skip設定）。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RetireeDeleteJobConfig {

    private static final int CHUNK_SIZE = 1;
    private static final int SKIP_LIMIT = Integer.MAX_VALUE;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EmployeeService employeeService;
    private final RetireeDeleteService retireeDeleteService;

    // ------------------------------------------------------------------
    // Job / Step
    // ------------------------------------------------------------------

    @Bean
    public Job retireeDeleteJob() {
        return new JobBuilder("retireeDeleteJob", jobRepository)
                .listener(retireeBatchLogListener())
                .start(retireeDeleteStep())
                .build();
    }

    @Bean
    public Step retireeDeleteStep() {
        return new StepBuilder("retireeDeleteStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(retireeItemReader(null))
                .writer(retireeItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(SKIP_LIMIT)
                .build();
    }

    // ------------------------------------------------------------------
    // Reader：削除対象社員のemployeeIdを一括取得して1件ずつ返す
    // ------------------------------------------------------------------

    @Bean
    @StepScope
    public ItemReader<Long> retireeItemReader(@Value("#{jobParameters['cutoffDate']}") String cutoffDateParam) {
        LocalDate cutoffDate = LocalDate.parse(cutoffDateParam);
        return new ListItemReader<>(employeeService.findRetireeIds(cutoffDate));
    }

    // ------------------------------------------------------------------
    // Writer：受け取ったemployeeIdの削除処理を実行する
    // ------------------------------------------------------------------

    @Bean
    public ItemWriter<Long> retireeItemWriter() {
        return chunk -> {
            for (Long employeeId : chunk) {
                retireeDeleteService.deleteEmployeeData(employeeId);
            }
        };
    }

    // ------------------------------------------------------------------
    // Listener：Job開始・終了時のログ出力
    // ------------------------------------------------------------------

    @Bean
    public JobExecutionListener retireeBatchLogListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("退職者削除バッチを開始します。cutoffDate={}",
                        jobExecution.getJobParameters().getString("cutoffDate"));
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                long deleteCount = jobExecution.getStepExecutions().stream()
                        .mapToLong(StepExecution::getWriteCount)
                        .sum();
                long skipCount = jobExecution.getStepExecutions().stream()
                        .mapToLong(StepExecution::getSkipCount)
                        .sum();

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("退職者削除バッチが正常終了しました。削除件数={}, スキップ件数={}", deleteCount, skipCount);
                } else {
                    log.error("退職者削除バッチが異常終了しました。ステータス={}, 削除件数={}, スキップ件数={}",
                            jobExecution.getStatus(), deleteCount, skipCount);
                }
            }
        };
    }
}