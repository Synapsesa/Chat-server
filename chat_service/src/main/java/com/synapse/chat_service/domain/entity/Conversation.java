package com.synapse.chat_service.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.synapse.chat_service.domain.common.BaseTimeEntity;

/**
 * 사용자와 AI 간의 1:1 대화를 나타내는 엔티티
 * 각 사용자는 하나의 대화(Conversation)를 가지며, 이는 자동으로 생성됩니다.
 * MSA 원칙에 따라 외부 서비스의 userId만을 참조하여 사용자 정보를 식별합니다.
 */
@Entity
@Table(name = "conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "conversation_id", columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();
    
    @Builder
    public Conversation(Long userId) {
        this.userId = userId;
    }
}
