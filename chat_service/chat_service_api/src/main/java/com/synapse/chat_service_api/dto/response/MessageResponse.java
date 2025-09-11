package com.synapse.chat_service_api.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageResponse {
    public record ConversationInfo(
        UUID conversationId,
        UUID userId,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate
    ) {
        public static ConversationInfo to(UUID conversionId, UUID memberId, LocalDateTime createdDate, LocalDateTime lastModifiedDate) {
            return new ConversationInfo(
                conversionId,
                memberId,
                createdDate,
                lastModifiedDate
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
        String senderType,
        String content,
        LocalDateTime createdDate
    ) {
        public static History to(Long messageId, UUID conversionId, String senderType, String content, LocalDateTime createdDate) {
            return new History(
                messageId,
                conversionId,
                senderType,
                content,
                createdDate
            );
        }
    }

    /**
     * WebSocket을 통해 AI 응답을 클라이언트에게 전송하는 DTO
     */
    public record Chat(
        String response,
        String modelType,
        String sessionId,
        String messageId,
        LocalDateTime timestamp,
        ResponseStatus status,
        String errorMessage
    ) {
        public static Chat success(String response, String modelType, String sessionId, String messageId) {
            return new Chat(
                response, 
                modelType,
                sessionId, 
                messageId, 
                LocalDateTime.now(), 
                ResponseStatus.SUCCESS,
                null
            );
        }

        public static Chat error(String modelType, String sessionId, String messageId, String errorMessage) {
            return new Chat(
                null, 
                modelType, 
                sessionId, 
                messageId, 
                LocalDateTime.now(), 
                ResponseStatus.ERROR,
                errorMessage
            );
        }
    }

    public enum ResponseStatus {
        SUCCESS, ERROR
    }
}
