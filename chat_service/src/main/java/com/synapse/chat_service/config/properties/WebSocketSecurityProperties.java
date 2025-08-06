package com.synapse.chat_service.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "websocket.security")
@Getter
@Setter
public class WebSocketSecurityProperties {
    private List<String> allowedOrigins;
    private boolean csrfProtection = true;
}
