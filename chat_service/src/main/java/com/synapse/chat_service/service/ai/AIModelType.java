package com.synapse.chat_service.service.ai;

/**
 * 지원하는 AI 모델 타입을 정의하는 Enum
 * 새로운 AI 모델 추가 시 이 Enum에 추가하여 일관성 있게 관리
 */
public enum AIModelType {
    GPT,
    CLAUDE,
    GEMINI
}
