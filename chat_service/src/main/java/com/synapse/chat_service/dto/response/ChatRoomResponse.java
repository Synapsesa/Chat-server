package com.synapse.chat_service.dto.response;

import com.synapse.chat_service.domain.entity.ChatRoom;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatRoomResponse {
    
    /**
     * 채팅방 목록 조회용 간단한 정보
     * 목록에서 필요한 최소한의 정보만 포함
     */
    public record Simple(
        UUID id,
        String title,
        LocalDateTime createdDate,
        long messageCount
    ) {
        public static Simple from(ChatRoom chatRoom, long messageCount) {
            return new Simple(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getCreatedDate(),
                messageCount
            );
        }
        
        public static Simple from(ChatRoom chatRoom) {
            return new Simple(
                chatRoom.getId(),
                chatRoom.getTitle(),
                chatRoom.getCreatedDate(),
                chatRoom.getMessages().size()
            );
        }
    }
    
    /**
     * 채팅방 상세 조회용 완전한 정보
     * 단일 채팅방 조회 시 필요한 모든 정보 포함
     */
    public record Detail(
        UUID id,
        Long userId,
        String title,
        LocalDateTime createdDate,
        LocalDateTime updatedDate,
        long messageCount
    ) {
        public static Detail from(ChatRoom chatRoom, long messageCount) {
            return new Detail(
                chatRoom.getId(),
                chatRoom.getUserId(),
                chatRoom.getTitle(),
                chatRoom.getCreatedDate(),
                chatRoom.getUpdatedDate(),
                messageCount
            );
        }
    }
}
