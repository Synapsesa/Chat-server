package com.synapse.chat_service.service.ai;

import java.util.concurrent.CompletableFuture;

/**
 * AI 모델 서비스의 공통 인터페이스
 * 각 AI 모델별 서비스는 이 인터페이스를 구현하여 일관된 API를 제공
 */
public interface AIModelService {
    
    /**
     * 해당 서비스가 지원하는 AI 모델 타입을 반환
     * @return 지원하는 AIModelType
     */
    AIModelType getModelType();
    
    /**
     * 주어진 프롬프트에 대해 AI 응답을 비동기적으로 생성
     * @param prompt 사용자 입력 프롬프트
     * @return AI 응답을 담은 CompletableFuture
     */
    CompletableFuture<String> generateResponse(String prompt);
}