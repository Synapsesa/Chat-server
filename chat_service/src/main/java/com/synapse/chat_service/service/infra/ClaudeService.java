package com.synapse.chat_service.service.infra;

import java.util.concurrent.CompletableFuture;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.synapse.chat_service.service.ai.AIModelService;
import com.synapse.chat_service.service.ai.AIModelType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeService implements AIModelService {

    private final AnthropicChatModel anthropicChatModel;

    @Override
    public AIModelType getModelType() {
        return AIModelType.CLAUDE;
    }

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> generateResponse(String prompt) {
        log.debug("Claude 모델로 응답 생성 시작 - prompt length: {}", prompt.length());
        try {
            String response = anthropicChatModel.call(prompt);
            log.debug("Claude 응답 생성 완료 - response length: {}", response != null ? response.length() : "null");
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Claude 응답 생성 실패", e);
            return CompletableFuture.failedFuture(new RuntimeException("Claude 모델 응답 생성 중 오류가 발생했습니다.", e));
        }
    }
}
