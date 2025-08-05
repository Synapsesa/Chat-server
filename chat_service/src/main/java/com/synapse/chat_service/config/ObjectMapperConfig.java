package com.synapse.chat_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * ObjectMapper 설정 클래스
 * 
 * 보안 고려사항:
 * - Default Typing은 안전하지 않은 역직렬화 취약점을 유발할 수 있어 비활성화
 * - 다형성 타입 처리가 필요한 경우 @JsonTypeInfo와 @JsonSubTypes 어노테이션을 
 * - 해당 클래스에 직접 사용하는 것을 권장
 */
@Configuration
public class ObjectMapperConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); 

        return objectMapper;
    }
}
