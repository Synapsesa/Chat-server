package com.synapse.chat_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ChatRoomRequest {
    
    public record Create(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,
        
        @NotBlank(message = "채팅방 제목은 필수입니다.")
        @Size(min = 1, max = 255, message = "채팅방 제목은 1자 이상 255자 이하여야 합니다.")
        String title
    ) {}
    
    public record Update(
        @NotBlank(message = "채팅방 제목은 필수입니다.")
        @Size(min = 1, max = 255, message = "채팅방 제목은 1자 이상 255자 이하여야 합니다.")
        String title
    ) {}
}
