package com.synapse.chat_service.dto.request;

import com.synapse.chat_service.domain.entity.enums.SenderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MessageRequest {
    
    public record Create(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,
        
        @NotNull(message = "발신자 타입은 필수입니다.")
        SenderType senderType,
        
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
    ) {}

    public record Update(
        @NotBlank(message = "메시지 내용은 필수입니다.")
        String content
    ) {}
}
