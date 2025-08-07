package com.synapse.chat_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import com.synapse.chat_service.config.properties.WebSocketSecurityProperties;
import com.synapse.chat_service.exception.service.CustomStompErrorHandler;
import com.synapse.chat_service.interceptor.WebSocketChannelInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@EnableConfigurationProperties(WebSocketSecurityProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final CustomStompErrorHandler customStompErrorHandler;
    private final WebSocketChannelInterceptor webSocketChannelInterceptor;
    private final WebSocketSecurityProperties webSocketSecurityProperties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트에서 메시지를 받을 때 사용할 prefix
        config.setApplicationDestinationPrefixes("/app");
        // 클라이언트가 구독할 때 사용할 prefix (AI 응답 수신용)
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(heartbeatScheduler())
                .setHeartbeatValue(new long[] {10000, 10000});
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.setErrorHandler(customStompErrorHandler);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    webSocketSecurityProperties.getAllowedOrigins().get(0)
                );
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 들어오는 메시지를 처리하는 스레드 풀 설정
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8)
            .queueCapacity(50);
        registration.interceptors(webSocketChannelInterceptor);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 클라이언트로 나가는 메시지를 처리하는 스레드 풀 설정
        registration.taskExecutor()
            .corePoolSize(4)
            .maxPoolSize(8)
            .queueCapacity(50);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
            .setMessageSizeLimit(128 * 1024) // 128KB
            .setSendTimeLimit(15 * 1000) // 15초
            .setSendBufferSizeLimit(512 * 1024); // 512KB
    }

    @Bean
    public ThreadPoolTaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        return scheduler;
    }
}
