package com.synapse.chat_service.session;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

/**
 * 세션 관련 설정 프로퍼티
 * 
 * @param expirationHours 세션 만료 시간 (시간 단위)
 * @param maxSessionsPerUser 사용자당 최대 세션 수
 */
@Validated
@ConfigurationProperties(prefix = "session")
public record SessionProperties(
    @Min(value = 1, message = "세션 만료 시간은 최소 1시간 이상이어야 합니다.")
    int expirationHours,
    
    @Min(value = 1, message = "사용자당 최대 세션 수는 최소 1개 이상이어야 합니다.")
    int maxSessionsPerUser
) {
}
