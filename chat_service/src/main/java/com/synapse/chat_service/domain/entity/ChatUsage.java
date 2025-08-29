package com.synapse.chat_service.domain.entity;

import java.util.UUID;

import com.synapse.chat_service.domain.common.BaseTimeEntity;
import com.synapse.chat_service.domain.entity.enums.SubscriptionType;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자의 채팅 사용량 및 구독 정보를 관리하는 엔티티
 * MSA 원칙에 따라 외부 서비스의 userId만을 참조하여 사용자를 식별합니다.
 */
@Entity
@Table(name = "chat_usages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatUsage extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    private SubscriptionType subscriptionType;
    
    @Min(0)
    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;
    
    @Min(0)
    @Column(name = "message_limit", nullable = false)
    private Integer messageLimit;
    
    @Builder
    public ChatUsage(UUID userId, SubscriptionType subscriptionType, Integer messageLimit) {
        this.userId = userId;
        this.subscriptionType = subscriptionType;
        this.messageLimit = messageLimit;
        this.messageCount = 0;
    }
}
