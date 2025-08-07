package com.synapse.chat_service.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 설정
 * AI 모델 호출의 대용량 트래픽을 고려한 스레드 풀 구성
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * AI 서비스 전용 스레드 풀
     * 대규모 동시 요청을 처리할 수 있도록 설정
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);        // 기본 스레드 수
        executor.setMaxPoolSize(50);         // 최대 스레드 수
        executor.setQueueCapacity(100);      // 대기 큐 크기
        executor.setThreadNamePrefix("AI-"); // 스레드 이름 접두사
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
