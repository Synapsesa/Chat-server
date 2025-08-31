package com.synapse.chat_service_api.dto.session;

import java.time.LocalDateTime;

public record SessionInfo(
    String sessionId,
    String userId,
    LocalDateTime connectedAt,
    LocalDateTime lastActivityAt,
    SessionStatus status,
    String clientInfo
) {
    /**
     * 새로운 AI 채팅 세션 생성을 위한 팩토리 메서드
     */
    public static SessionInfo create(String sessionId, String userId, String clientInfo) {
        LocalDateTime now = LocalDateTime.now();
        return new SessionInfo(
            sessionId,
            userId,
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
            connectedAt,
            LocalDateTime.now(),
            newStatus,
            clientInfo
        );
    }
}
