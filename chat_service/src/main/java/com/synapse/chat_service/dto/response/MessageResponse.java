package com.synapse.chat_service.dto.response;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.service.ai.AIModelType;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    public record ConversationInfo(
        UUID conversationId,
        UUID userId,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
    ) {
        public static ConversationInfo from(Conversation conversation) {
            return new ConversationInfo(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getCreatedDate(),
                conversation.getUpdatedDate()
            );
        }
    }
    
    /**
     * 대화 히스토리 조회용 응답 DTO
     * 페이징 처리된 메시지 목록에서 사용
     */
    public record History(
        Long id,
        UUID conversationId,
        SenderType senderType,
        String content,
        LocalDateTime createdDate
    ) {
        public static History from(Message message) {
            return new History(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedDate()
            );
        }
    }
    
    /**
     * WebSocket을 통해 AI 응답을 클라이언트에게 전송하는 DTO
     */
    public record Chat(
        String response,
        AIModelType modelType,
        String sessionId,
        String messageId,
        LocalDateTime timestamp,
        ResponseStatus status,
        String errorMessage
    ) {
        public static Chat success(String response, AIModelType modelType, String sessionId, String messageId) {
            return new Chat(response, modelType, sessionId, messageId, LocalDateTime.now(), ResponseStatus.SUCCESS, null);
        }
        
        public static Chat error(AIModelType modelType, String sessionId, String messageId, String errorMessage) {
            return new Chat(null, modelType, sessionId, messageId, LocalDateTime.now(), ResponseStatus.ERROR, errorMessage);
        }
    }
    
    public enum ResponseStatus {
        SUCCESS, ERROR
    }
}
