package com.synapse.chat_service.dto.response;

import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    
    public record Simple(
        Long id,
        UUID conversationId,
        SenderType senderType,
        String content,
        LocalDateTime createdDate
    ) {
        public static Simple from(Message message) {
            return new Simple(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedDate()
            );
        }
    }
    
    public record Detail(
        Long id,
        UUID conversationId,
        SenderType senderType,
        String content,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
    ) {
        public static Detail from(Message message) {
            return new Detail(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedDate(),
                message.getUpdatedDate()
            );
        }
    }
}
