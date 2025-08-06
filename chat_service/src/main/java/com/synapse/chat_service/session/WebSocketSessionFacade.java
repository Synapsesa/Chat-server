package com.synapse.chat_service.session;

import com.synapse.chat_service.session.dto.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * AI 채팅 WebSocket 세션 관리를 위한 Facade 클래스
 * AI와의 1:1 채팅에 최적화된 간단한 세션 관리 로직을 제공합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionFacade {
    
    private final RedisSessionManager sessionManager;
    private final RedisAiChatManager aiChatManager;
    
    /**
     * 사용자 연결 처리
     * 1. 새로운 세션 생성 (다중 기기 동시 접속 지원)
     * 2. AI 채팅 정보 조회 (MessageService에서 DB와 Redis 동기화 처리)
     */
    public SessionInfo handleUserConnection(String sessionId, String userId, String clientInfo) {
        log.info("AI 채팅 사용자 연결 처리 시작: sessionId={}, userId={}", sessionId, userId);
        
        // 1. 새로운 세션 생성 (다중 세션 지원)
        SessionInfo sessionInfo = SessionInfo.create(sessionId, userId, clientInfo);
        sessionManager.createSession(sessionInfo);
        
        // 2. AI 채팅 정보 조회 (MessageService에서 DB와 Redis 동기화가 이미 처리됨)
        // 기존 정보가 있으면 활동 시간만 업데이트
        aiChatManager.updateAiChatActivity(userId);
        
        log.info("AI 채팅 사용자 연결 처리 완료: sessionId={}, userId={}", sessionId, userId);
        
        return sessionInfo;
    }
    
    /**
     * 사용자 연결 해제 처리
     * 1. 세션 정보 조회
     * 2. AI 채팅 활동 시간 업데이트
     * 3. 세션 삭제
     */
    public void handleUserDisconnection(String sessionId) {
        log.info("AI 채팅 사용자 연결 해제 처리 시작: sessionId={}", sessionId);
        
        // 1. 세션 정보 조회
        SessionInfo sessionInfo = sessionManager.getSession(sessionId);
        if (sessionInfo == null) {
            log.warn("연결 해제 시 세션을 찾을 수 없음: sessionId={}", sessionId);
            return;
        }
        
        // 2. AI 채팅 활동 시간 업데이트 (rethrowException = false)
        aiChatManager.updateAiChatActivity(sessionInfo.userId());
        
        // 3. 세션 삭제
        sessionManager.deleteSession(sessionId);
        
        log.info("AI 채팅 사용자 연결 해제 처리 완료: sessionId={}, userId={}", 
                sessionId, sessionInfo.userId());
    }
    
    /**
     * 메시지 활동 처리
     * 1. AI 채팅 메시지 수 증가
     * 2. AI 채팅 활동 시간 업데이트
     */
    public void handleMessageActivity(String userId) {
        log.debug("AI 채팅 메시지 활동 처리: userId={}", userId);
        
        // 1. AI 채팅 메시지 수 증가 (rethrowException = false)
        aiChatManager.incrementMessageCount(userId);
        
        // 2. AI 채팅 활동 시간 업데이트 (rethrowException = false)
        aiChatManager.updateAiChatActivity(userId);
    }

    /**
     * 세션 활동 업데이트
     */
    public void updateSessionActivity(String sessionId) {
        log.debug("세션 활동 업데이트: sessionId={}", sessionId);
        
        SessionInfo sessionInfo = sessionManager.getSession(sessionId);
        if (sessionInfo != null) {
            SessionInfo updatedSession = sessionInfo.updateLastActivity();
            sessionManager.updateSession(updatedSession);
        }
    }
    
    /**
     * 사용자의 대화 ID 조회
     */
    public String getConversationId(String userId) {
        return aiChatManager.getAiChat(userId)
                .map(aiChat -> aiChat.conversationId().toString())
                .orElse("ai-chat-" + userId); // 기본 패턴 반환 (호환성 유지)
    }
    
    /**
     * 사용자의 모든 세션 강제 삭제 (관리자 기능)
     */
    public void forceDeleteAllUserSessions(String userId) {
        log.info("사용자 모든 세션 강제 삭제: userId={}", userId);
        // deleteAllUserSessions는 rethrowException = false로 설정되어 예외를 던지지 않음
        sessionManager.deleteAllUserSessions(userId);
    }
}
