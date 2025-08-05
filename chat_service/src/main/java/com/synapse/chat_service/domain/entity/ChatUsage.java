package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.common.BaseTimeEntity;
import com.synapse.chat_service.domain.entity.enums.SubscriptionType;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
    
    @Column(name = "user_id", nullable = false, unique = true)
    @NotNull
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_type", nullable = false)
    @NotNull
    private SubscriptionType subscriptionType;
    
    @Column(name = "message_count", nullable = false)
    @Min(0)
    private Integer messageCount = 0;
    
    @Column(name = "message_limit", nullable = false)
    @Min(0)
    private Integer messageLimit;
    
    @Builder
    public ChatUsage(Long userId, SubscriptionType subscriptionType, Integer messageLimit) {
        this.userId = userId;
        this.subscriptionType = subscriptionType;
        this.messageLimit = messageLimit;
        this.messageCount = 0;
    }
}
