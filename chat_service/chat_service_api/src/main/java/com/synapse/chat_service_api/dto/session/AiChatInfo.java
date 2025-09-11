package com.synapse.chat_service_api.dto.session;

import java.time.LocalDateTime;
import java.util.UUID;

public record AiChatInfo(
    String userId,
    UUID conversationId,
    LocalDateTime createdAt,
    LocalDateTime lastActivityAt,
    Long messageCount
) {

    /**
     * 새로운 AI 채팅 생성을 위한 팩토리 메서드
     * 실제 데이터베이스의 Conversation UUID를 사용하여 Redis와 DB 간 일관성을 보장합니다.
     */
    public static AiChatInfo create(String userId, UUID conversationId) {
        LocalDateTime now = LocalDateTime.now();

        return new AiChatInfo(
            userId,
            conversationId,
            now,
            now,
            0L
        );
    }

    /**
     * 마지막 활동 시간 업데이트
     */
    public AiChatInfo updateLastActivity() {
        return new AiChatInfo(
            userId,
            conversationId,
            createdAt,
            LocalDateTime.now(),
            messageCount
        );
    }

    /**
     * 메시지 수 증가
     */
    public AiChatInfo incrementMessageCount() {
        return new AiChatInfo(
            userId,
            conversationId,
            createdAt,
            LocalDateTime.now(),
            messageCount + 1
        );
    }
}
