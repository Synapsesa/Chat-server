package com.synapse.chat_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ChatMessageRequest(
    
    @Min(value = 1, message = "조회할 개수는 1 이상이어야 합니다")
    @Max(value = 100, message = "조회할 개수는 100 이하여야 합니다")
    Integer size,
    
    String cursor
) {
    public ChatMessageRequest {
        if (size == null) {
            size = 20;
        }
    }
}
