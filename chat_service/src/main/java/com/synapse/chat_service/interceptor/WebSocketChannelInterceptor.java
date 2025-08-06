package com.synapse.chat_service.interceptor;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.synapse.chat_service.provider.JwtTokenProvider;
import com.synapse.chat_service.session.WebSocketSessionFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * STOMP 프로토콜 레벨에서 메시지를 가로채는 인터셉터
 * 세션 생명주기 관리 및 활동 추적 등 인증/상태와 관련된 부가적인 처리를 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;
    private final WebSocketSessionFacade webSocketSessionFacade;

    /**
     * STOMP 메시지 전송 전 처리
     * CONNECT, DISCONNECT, MESSAGE(SEND) 프레임을 가로채어 세션 관리 및 활동 추적을 수행합니다.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> handleConnect(accessor);
            case DISCONNECT -> handleDisconnect(accessor);
            case SEND -> handleMessage(accessor);
            default -> {}
        }

        return message;
    }

    /**
     * CONNECT 프레임 처리
     * 사용자 인증 정보를 확인하고 WebSocket 세션을 생성합니다.
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String jwtToken = accessor.getFirstNativeHeader("Authorization");
            
        Authentication authentication = jwtTokenProvider.verifyAndDecode(jwtToken);
        // STOMP 세션에 인증 정보 저장
        accessor.setUser(authentication);
        
        String sessionId = accessor.getSessionId();
        String userId = authentication.getName();
        
        if (userId == null) {
            log.warn("CONNECT 프레임에서 인증되지 않은 사용자 감지: sessionId={}", sessionId);
            return;
        }

        String clientInfo = extractClientInfo(accessor);

        log.info("CONNECT 프레임 처리: sessionId={}, userId={}", sessionId, userId);

        // WebSocket 세션 생성 및 AI 채팅 정보 동기화
        webSocketSessionFacade.handleUserConnection(sessionId, userId, clientInfo);

        // 세션 속성에 사용자 ID 저장 (DISCONNECT 시 활용)
        accessor.getSessionAttributes().put("userId", userId);
    }

    /**
     * DISCONNECT 프레임 처리
     * WebSocket 세션을 정리합니다.
     */
    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String userId = (String) accessor.getSessionAttributes().get("userId");
        
        log.info("DISCONNECT 프레임 처리: sessionId={}, userId={}", sessionId, userId);

        // WebSocket 세션 정리
        webSocketSessionFacade.handleUserDisconnection(sessionId);
    }

    /**
     * MESSAGE(SEND) 프레임 처리
     * 사용자의 메시지 활동을 추적하고 활동 시간을 업데이트합니다.
     */
    private void handleMessage(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String userId = (String) accessor.getSessionAttributes().get("userId");
        
        if (userId == null) {
            log.warn("MESSAGE 프레임에서 사용자 ID를 찾을 수 없음: sessionId={}", sessionId);
            return;
        }

        log.debug("MESSAGE 프레임 처리: sessionId={}, userId={}", sessionId, userId);

        // 메시지 활동 추적 (마지막 활동 시간 및 메시지 수 업데이트)
        webSocketSessionFacade.handleMessageActivity(userId);
    }

    private String extractClientInfo(StompHeaderAccessor accessor) {
        try {
            StringBuilder clientInfo = new StringBuilder();
            
            String clientType = accessor.getFirstNativeHeader("client-type");
            if (clientType != null) {
                clientInfo.append("Type: ").append(clientType);
            }
            
            String clientVersion = accessor.getFirstNativeHeader("client-version");
            if (clientVersion != null) {
                if (clientInfo.length() > 0) clientInfo.append(", ");
                clientInfo.append("Version: ").append(clientVersion);
            }
            
            String deviceInfo = accessor.getFirstNativeHeader("device-info");
            if (deviceInfo != null) {
                if (clientInfo.length() > 0) clientInfo.append(", ");
                clientInfo.append("Device: ").append(deviceInfo);
            }
            
            // 기본값: 클라이언트 정보가 없는 경우
            return clientInfo.length() > 0 ? clientInfo.toString() : "Web Client";
            
        } catch (Exception e) {
            log.warn("Failed to extract client info from STOMP headers: {}", e.getMessage());
            return "Unknown Client";
        }
    }
}
