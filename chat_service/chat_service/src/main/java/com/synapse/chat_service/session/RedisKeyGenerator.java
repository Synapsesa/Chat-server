package com.synapse.chat_service.session;

import org.springframework.stereotype.Component;

@Component
public class RedisKeyGenerator {

    // 키 접두사 상수
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final String AI_CONVERSATION_PREFIX = "ai:conversation:";

    /**
     * WebSocket 세션 키 생성
     * 
     * @param sessionId WebSocket 세션 ID
     * @return Redis 키 (예: "session:abc123")
     */
    public String generateSessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    /**
     * 사용자별 세션 키 생성
     * 
     * @param userId 사용자 ID
     * @return Redis 키 (예: "user:session:user123")
     */
    public String generateUserSessionKey(String userId) {
        return USER_SESSION_PREFIX + userId;
    }

    /**
     * AI 대화 세션 키 생성
     * 
     * @param userId 사용자 ID
     * @return Redis 키 (예: "ai:conversation:user123")
     */
    public String generateAIConversationKey(String userId) {
        return AI_CONVERSATION_PREFIX + userId;
    }

    /**
     * AI 채팅 정보 키 생성
     * 패턴: "ai:chat:{userId}"
     */
    public String generateAiChatKey(String userId) {
        return "ai:chat:" + userId;
    }

    /**
     * 패턴 매칭을 위한 와일드카드 키 생성
     * 
     * @param prefix 접두사
     * @return 와일드카드 패턴 (예: "session:*")
     */
    public String generatePatternKey(String prefix) {
        return prefix + "*";
    }

    /**
     * 모든 세션 키 패턴
     * 
     * @return "session:*"
     */
    public String getAllSessionsPattern() {
        return generatePatternKey(SESSION_PREFIX);
    }
}
