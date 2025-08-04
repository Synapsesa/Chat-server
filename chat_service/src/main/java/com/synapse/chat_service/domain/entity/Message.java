package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.common.BaseTimeEntity;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.exception.commonexception.ValidException;
import com.synapse.chat_service.exception.domain.ExceptionType;

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
public class Message extends BaseTimeEntity {
    
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
        validateContent(content);
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
    }
    
    /**
     * 메시지 내용 업데이트 (도메인 로직)
     * @param newContent 새로운 메시지 내용
     */
    public void updateContent(String newContent) {
        validateContent(newContent);
        this.content = newContent;
    }
    
    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ValidException(ExceptionType.INVALID_INPUT_VALUE, "메시지 내용은 비어있을 수 없습니다.");
        }
        
        if (content.length() > 1000) {
            throw new ValidException(ExceptionType.INVALID_INPUT_VALUE, "메시지 내용은 1000자를 초과할 수 없습니다.");
        }
    }
}
