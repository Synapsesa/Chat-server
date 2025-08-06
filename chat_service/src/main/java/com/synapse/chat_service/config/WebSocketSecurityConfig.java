package com.synapse.chat_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.context.annotation.Bean;

import static org.springframework.messaging.simp.SimpMessageType.CONNECT;
import static org.springframework.messaging.simp.SimpMessageType.CONNECT_ACK;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT;
import static org.springframework.messaging.simp.SimpMessageType.HEARTBEAT;
import static org.springframework.messaging.simp.SimpMessageType.UNSUBSCRIBE;
import static org.springframework.messaging.simp.SimpMessageType.DISCONNECT_ACK;

@Configuration(proxyBeanMethods = false)
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {
    private static final String ASSISTANT_SUBSCRIBE_DEST = "/assistant/**";
    private static final String MESSAGE_DEST = "/app/v1/message/**";

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager() {
        MessageMatcherDelegatingAuthorizationManager.Builder builder = 
                new MessageMatcherDelegatingAuthorizationManager.Builder();
        
        return builder
                // 연결 요청은 인증된 사용자만 허용
                .simpTypeMatchers(
                        CONNECT,
                        CONNECT_ACK,
                        HEARTBEAT,
                        UNSUBSCRIBE
                ).authenticated()
                // 구독 요청 권한 설정 (AI 응답 수신용)
                .simpSubscribeDestMatchers(ASSISTANT_SUBSCRIBE_DEST).authenticated()
                // 메시지 전송 권한 설정 (AI 채팅 통합)
                .simpMessageDestMatchers(MESSAGE_DEST).authenticated()
                // 연결 해제는 모든 사용자 허용
                .simpTypeMatchers(
                        DISCONNECT,
                        DISCONNECT_ACK
                ).permitAll()
                .anyMessage().authenticated()
                .build();
    }
}
