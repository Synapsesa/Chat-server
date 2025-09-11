package com.synapse.chat_service.domain.entity;

import java.util.UUID;

import com.synapse.chat_service.domain.common.BaseTimeEntity;
import com.synapse.chat_service.domain.entity.enums.SubscriptionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
