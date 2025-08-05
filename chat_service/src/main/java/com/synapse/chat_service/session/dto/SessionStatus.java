package com.synapse.chat_service.session.dto;

/**
 * WebSocket 세션의 상태를 나타내는 열거형
 */
public enum SessionStatus {
    
    /**
     * 연결된 상태 - 정상적으로 WebSocket 연결이 활성화된 상태
     */
    CONNECTED,
    
    /**
     * 연결 해제된 상태 - WebSocket 연결이 종료된 상태
     */
    DISCONNECTED,
    
    /**
     * 유휴 상태 - 연결은 유지되지만 일정 시간 동안 활동이 없는 상태
     */
    IDLE,
    
    /**
     * 재연결 중 상태 - 네트워크 문제 등으로 재연결을 시도하는 상태
     */
    RECONNECTING
}
