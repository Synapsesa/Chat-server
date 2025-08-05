package com.synapse.chat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트에서 메시지를 받을 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
        
        // 클라이언트가 구독할 때 사용할 prefix
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(heartbeatScheduler())
                .setHeartbeatValue(new long[] {10000, 10000});
        
        // AI 응답을 특정 사용자에게 보낼 때 사용할 prefix
        config.setUserDestinationPrefix("/ai");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");  // CORS 설정: 모든 도메인 허용 (개발 환경)
    }

    @Bean
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }
}
