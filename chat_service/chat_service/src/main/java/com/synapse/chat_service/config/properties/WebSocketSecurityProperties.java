package com.synapse.chat_service.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "websocket.security")
@Getter
@Setter
public class WebSocketSecurityProperties {
    private List<String> allowedOrigins;
    private boolean csrfProtection = true;
}
