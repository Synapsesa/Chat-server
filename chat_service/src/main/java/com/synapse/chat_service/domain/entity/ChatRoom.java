package com.synapse.chat_service.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.synapse.chat_service.exception.commonexception.ValidException;
import com.synapse.chat_service.exception.domain.ExceptionType;

import com.synapse.chat_service.domain.common.BaseTimeEntity;

@Entity
@Table(name = "chat_rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "chat_room_id", columnDefinition = "UUID")
    private UUID id;
    
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @NotBlank
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();
    
    @Builder
    public ChatRoom(Long userId, String title) {
        this.userId = userId;
        this.title = title;
    }
    
    public void updateTitle(String newTitle) {
        validateTitle(newTitle);
        this.title = newTitle.trim();
    }
    
    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ValidException(ExceptionType.INVALID_INPUT_VALUE, "채팅방 제목은 비어있을 수 없습니다.");
        }
        if (title.trim().length() > 255) {
            throw new ValidException(ExceptionType.INVALID_INPUT_VALUE, "채팅방 제목은 255자를 초과할 수 없습니다.");
        }
    }
}
