package com.synapse.chat_service.session.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI 채팅 정보를 저장하는 Record
 * Redis에서 현재 활성화된 WebSocket 세션과 관련된 상태 정보 및 캐시 역할을 합니다.
 * 데이터베이스의 Conversation 엔티티가 영구적인 저장소(Source of Truth) 역할을 하며,
 * 이 레코드는 자주 접근하지만 휘발되어도 괜찮은 메타데이터를 저장하여 DB 조회를 줄입니다.
 * 
 * @param userId 사용자 ID
 * @param conversationId 실제 데이터베이스의 Conversation UUID (Redis와 DB 간 일관성 보장)
 * @param createdAt 채팅방 생성 시간
 * @param lastActivityAt 마지막 활동 시간
 * @param messageCount 총 메시지 수 (선택적 통계)
 */
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
