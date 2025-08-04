package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.repository.ChatRoomRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.dto.response.MessageResponse;
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
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    
    @Transactional
    public MessageResponse.Detail createMessage(MessageRequest.Create request) {
        ChatRoom chatRoom = findChatRoomById(request.chatRoomId());
        
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderType(request.senderType())
                .content(request.content())
                .build();
        
        Message savedMessage = messageRepository.save(message);
        return MessageResponse.Detail.from(savedMessage);
    }
    
    public MessageResponse.Detail getMessage(Long messageId) {
        Message message = findMessageById(messageId);
        return MessageResponse.Detail.from(message);
    }
    
    public List<MessageResponse.Simple> getMessagesByChatRoomId(UUID chatRoomId) {
        // 채팅방 존재 여부 확인
        findChatRoomById(chatRoomId);
        
        List<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(chatRoomId);
        return messages.stream()
                .map(MessageResponse.Simple::from)
                .collect(Collectors.toList());
    }
    
    public Page<MessageResponse.Simple> getMessagesByChatRoomIdWithPaging(UUID chatRoomId, Pageable pageable) {
        // 채팅방 존재 여부 확인
        findChatRoomById(chatRoomId);
        
        Page<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedDateAsc(chatRoomId, pageable);
        return messages.map(MessageResponse.Simple::from);
    }
    
    public Page<MessageResponse.Simple> getMessagesRecentFirst(UUID chatRoomId, Pageable pageable) {
        // 채팅방 존재 여부 확인
        findChatRoomById(chatRoomId);
        
        Page<Message> messages = messageRepository.findByChatRoomIdOrderByCreatedDateDesc(chatRoomId, pageable);
        return messages.map(MessageResponse.Simple::from);
    }
    
    public List<MessageResponse.Simple> searchMessages(UUID chatRoomId, String keyword) {
        // 채팅방 존재 여부 확인
        findChatRoomById(chatRoomId);
        
        List<Message> messages = messageRepository.findByChatRoomIdAndContentContaining(chatRoomId, keyword);
        return messages.stream()
                .map(MessageResponse.Simple::from)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void deleteMessage(Long messageId) {
        Message message = findMessageById(messageId);
        messageRepository.delete(message);
    }
    
    public long getMessageCount(UUID chatRoomId) {
        // 채팅방 존재 여부 확인
        findChatRoomById(chatRoomId);
        
        return messageRepository.countByChatRoomId(chatRoomId);
    }
    
    private ChatRoom findChatRoomById(UUID chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new NotFoundException(ExceptionType.CHAT_ROOM_NOT_FOUND, "ID: " + chatRoomId));
    }
    
    private Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException(ExceptionType.MESSAGE_NOT_FOUND, "ID: " + messageId));
    }
}
