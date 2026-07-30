package com.example.backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.RetireeDeleteResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private static final int RETENTION_MONTHS = 3;

    private final JobLauncher jobLauncher;
    private final Job retireeDeleteJob;

    @PostMapping("/retiree-delete")
    public ResponseEntity<RetireeDeleteResult> triggerRetireeDelete() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusMonths(RETENTION_MONTHS);
            var jobParameters = new JobParametersBuilder()
                    .addString("cutoffDate", cutoffDate.toString())
                    .addLocalDateTime("executedAt", LocalDateTime.now())
                    .toJobParameters();
            jobLauncher.run(retireeDeleteJob, jobParameters);
            return ResponseEntity.ok(new RetireeDeleteResult("STARTED", "退職者削除バッチを実行しました。"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new RetireeDeleteResult("FAILED", e.getMessage()));
        }
    }
}
