package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.common.BaseTimeEntity;
import com.synapse.chat_service.domain.entity.enums.SenderType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @NotNull
    private Conversation conversation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    @NotNull
    private SenderType senderType;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Builder
    public Message(Conversation conversation, SenderType senderType, String content) {
        this.conversation = conversation;
        this.senderType = senderType;
        this.content = content;
    }
}
