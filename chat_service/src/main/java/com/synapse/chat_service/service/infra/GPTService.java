package com.synapse.chat_service.service.infra;

import java.util.concurrent.CompletableFuture;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.synapse.chat_service.service.ai.AIModelService;
import com.synapse.chat_service.service.ai.AIModelType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OpenAI GPT 모델을 사용하는 AI 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService implements AIModelService {

    private final OpenAiChatModel openAiChatModel;

    @Override
    public AIModelType getModelType() {
        return AIModelType.GPT;
    }

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> generateResponse(String prompt) {
        log.debug("GPT 모델로 응답 생성 시작 - prompt length: {}", prompt.length());
        try {
            String response = openAiChatModel.call(prompt);
            log.debug("GPT 응답 생성 완료 - response length: {}", response.length());
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("GPT 응답 생성 실패", e);
            return CompletableFuture.failedFuture(new RuntimeException("GPT 모델 응답 생성 중 오류가 발생했습니다.", e));
        }
    }
}
