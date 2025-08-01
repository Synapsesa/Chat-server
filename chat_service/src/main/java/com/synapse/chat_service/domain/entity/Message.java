package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.common.BaseEntity;
import com.synapse.chat_service.domain.entity.enums.SenderType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    @NotNull
    private ChatRoom chatRoom;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    @NotNull
    private SenderType senderType;
    
    @NotBlank
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Builder
    public Message(ChatRoom chatRoom, SenderType senderType, String content) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
    }
}
