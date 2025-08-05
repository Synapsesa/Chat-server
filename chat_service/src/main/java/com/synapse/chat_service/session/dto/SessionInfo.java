package com.synapse.chat_service.session.dto;

import java.time.LocalDateTime;

/**
 * AI 채팅 WebSocket 세션 정보를 저장하는 Record
 * Redis에 JSON 형태로 직렬화되어 저장됩니다.
 * 
 * @param sessionId WebSocket 세션 ID
 * @param userId 사용자 ID
 * @param username 사용자 이름
 * @param connectedAt 세션 연결 시간
 * @param lastActivityAt 마지막 활동 시간
 * @param status 세션 상태 (CONNECTED, DISCONNECTED, IDLE)
 * @param clientInfo 클라이언트 정보 (브라우저, 모바일 앱 등)
 */
public record SessionInfo(
        String sessionId,
        String userId,
        String username,
        LocalDateTime connectedAt,
        LocalDateTime lastActivityAt,
        SessionStatus status,
        String clientInfo
) {
    
    /**
     * 새로운 AI 채팅 세션 생성을 위한 팩토리 메서드
     */
    public static SessionInfo create(String sessionId, String userId, String username, String clientInfo) {
        LocalDateTime now = LocalDateTime.now();
        return new SessionInfo(
                sessionId,
                userId,
                username,
                now,
                now,
                SessionStatus.CONNECTED,
                clientInfo
        );
    }
    
    /**
     * 마지막 활동 시간 업데이트
     */
    public SessionInfo updateLastActivity() {
        return new SessionInfo(
                sessionId,
                userId,
                username,
                connectedAt,
                LocalDateTime.now(),
                status,
                clientInfo
        );
    }
    
    /**
     * 세션 상태 변경
     */
    public SessionInfo changeStatus(SessionStatus newStatus) {
        return new SessionInfo(
                sessionId,
                userId,
                username,
                connectedAt,
                LocalDateTime.now(),
                newStatus,
                clientInfo
        );
    }
}
