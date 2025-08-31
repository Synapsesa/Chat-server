package com.synapse.chat_service_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MessageRequest {
    public record Chat(
        @NotNull(message = "AI 모델 타입은 필수입니다.") 
        String modelType,

        @NotBlank(message = "프롬프트 메시지는 필수입니다.") 
        String prompt,

        @NotBlank(message = "세션 ID는 필수입니다.")
        String sessionId,

        @NotBlank(message = "메시지 ID는 필수입니다.") 
        String messageId
    ) {

    }
}
