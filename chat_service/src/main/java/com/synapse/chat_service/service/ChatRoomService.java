package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.repository.ChatRoomRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.dto.request.ChatRoomRequest;
import com.synapse.chat_service.dto.response.ChatRoomResponse;
import com.synapse.chat_service.exception.commonexception.NotFoundException;
import com.synapse.chat_service.exception.domain.ExceptionType;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    
    @Transactional
    public ChatRoomResponse.Detail createChatRoom(ChatRoomRequest.Create request) {
        ChatRoom chatRoom = ChatRoom.builder()
                .userId(request.userId())
                .title(request.title())
                .build();
        
        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        long messageCount = messageRepository.countByChatRoomId(savedChatRoom.getId());
        return ChatRoomResponse.Detail.from(savedChatRoom, messageCount);
    }
    
    public ChatRoomResponse.Detail getChatRoom(UUID chatRoomId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        long messageCount = messageRepository.countByChatRoomId(chatRoomId);
        return ChatRoomResponse.Detail.from(chatRoom, messageCount);
    }
    
    public Page<ChatRoomResponse.Simple> getChatRoomsByUserId(Long userId, Pageable pageable) {
        Page<ChatRoom> chatRooms = chatRoomRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
        return chatRooms.map(chatRoom -> {
            long messageCount = messageRepository.countByChatRoomId(chatRoom.getId());
            return ChatRoomResponse.Simple.from(chatRoom, messageCount);
        });
    }
    
    @Transactional
    public ChatRoomResponse.Detail updateChatRoom(UUID chatRoomId, ChatRoomRequest.Update request) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        
        // 명확한 의도를 가진 메소드를 통한 상태 변경 (도메인 로직에서 유효성 검증 포함)
        chatRoom.updateTitle(request.title());
        
        ChatRoom updatedChatRoom = chatRoomRepository.save(chatRoom);
        long messageCount = messageRepository.countByChatRoomId(chatRoomId);
        return ChatRoomResponse.Detail.from(updatedChatRoom, messageCount);
    }
    
    @Transactional
    public void deleteChatRoom(UUID chatRoomId) {
        ChatRoom chatRoom = findChatRoomById(chatRoomId);
        chatRoomRepository.delete(chatRoom);
    }
    
    public List<ChatRoomResponse.Simple> searchChatRooms(Long userId, String keyword) {
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserIdAndTitleContaining(userId, keyword);
        return chatRooms.stream()
                .map(chatRoom -> {
                    long messageCount = messageRepository.countByChatRoomId(chatRoom.getId());
                    return ChatRoomResponse.Simple.from(chatRoom, messageCount);
                })
                .collect(Collectors.toList());
    }
    
    private ChatRoom findChatRoomById(UUID chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException(ExceptionType.CHAT_ROOM_NOT_FOUND, "ID: " + chatRoomId));
    }
}
