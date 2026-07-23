package com.example.backend.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 日時取得用の共通設定。
 */
@Configuration
public class TimeConfig {

    /**
     * システム日時取得用のClockを登録する。
     *
     * @return システムデフォルトタイムゾーンのClock
     */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}