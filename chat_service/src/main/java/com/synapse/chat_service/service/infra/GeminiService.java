package com.synapse.chat_service.service.infra;

import java.util.concurrent.CompletableFuture;

import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.synapse.chat_service.service.ai.AIModelService;
import com.synapse.chat_service.service.ai.AIModelType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Google Gemini 모델을 사용하는 AI 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService implements AIModelService {

    private final VertexAiGeminiChatModel vertexAiGeminiChatModel;

    @Override
    public AIModelType getModelType() {
        return AIModelType.GEMINI;
    }

    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<String> generateResponse(String prompt) {
        log.debug("Gemini 모델로 응답 생성 시작 - prompt length: {}", prompt.length());
        try {
            String response = vertexAiGeminiChatModel.call(prompt);
            log.debug("Gemini 응답 생성 완료 - response length: {}", response.length());
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            log.error("Gemini 응답 생성 실패", e);
            return CompletableFuture.failedFuture(new RuntimeException("Gemini 모델 응답 생성 중 오류가 발생했습니다.", e));
        }
    }
}
