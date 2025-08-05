package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.repository.ConversationRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.exception.commonexception.NotFoundException;
import com.synapse.chat_service.exception.domain.ExceptionType;
import com.synapse.chat_service.session.RedisAiChatManager;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final RedisAiChatManager redisAiChatManager;
    
    @Transactional
    public MessageResponse.Detail createMessage(MessageRequest.Create request) {
        // 사용자의 대화가 존재하지 않으면 자동으로 생성
        Conversation conversation = getOrCreateConversation(request.userId());
        
        Message message = Message.builder()
                .conversation(conversation)
                .senderType(request.senderType())
                .content(request.content())
                .build();
        
        Message savedMessage = messageRepository.save(message);
        return MessageResponse.Detail.from(savedMessage);
    }
    
    /**
     * 사용자의 대화 조회 (공통 메소드)
     * 모든 conversation 조회 로직을 통합하여 중복을 제거
     */
    private Optional<Conversation> findConversationByUserId(Long userId) {
        return conversationRepository.findByUserId(userId);
    }
    
    /**
     * 사용자의 대화를 조회하거나 없으면 새로 생성
     * Redis의 AiChatInfo와 DB의 Conversation 간 일관성을 보장
     */
    private Conversation getOrCreateConversation(Long userId) {
        return findConversationByUserId(userId)
                .map(conversation -> {
                    // 기존 대화가 있으면 Redis 정보 동기화
                    redisAiChatManager.syncConversationId(userId.toString(), conversation.getId());
                    return conversation;
                })
                .orElseGet(() -> {
                    // 새로운 대화 생성
                    Conversation newConversation = Conversation.builder()
                            .userId(userId)
                            .build();
                    Conversation savedConversation = conversationRepository.save(newConversation);
                    
                    // Redis에 새로운 대화 정보 저장
                    redisAiChatManager.createOrUpdateAiChatWithConversation(
                            userId.toString(), 
                            savedConversation.getId()
                    );
                    
                    return savedConversation;
                });
    }
    
    public MessageResponse.Detail getMessage(Long messageId) {
        Message message = findMessageById(messageId);
        return MessageResponse.Detail.from(message);
    }
    
    public List<MessageResponse.Simple> getMessagesByUserId(Long userId) {
        // 사용자의 대화 조회 (없으면 빈 리스트 반환)
        return findConversationByUserId(userId)
                .map(conversation -> {
                    List<Message> messages = messageRepository.findByConversationIdOrderByCreatedDateAsc(conversation.getId());
                    return messages.stream()
                            .map(MessageResponse.Simple::from)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }
    
    public Page<MessageResponse.Simple> getMessagesByUserIdWithPaging(Long userId, Pageable pageable) {
        // 사용자의 대화 조회 (없으면 빈 페이지 반환)
        return findConversationByUserId(userId)
                .map(conversation -> {
                    Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedDateAsc(conversation.getId(), pageable);
                    return messages.map(MessageResponse.Simple::from);
                })
                .orElse(Page.empty(pageable));
    }
    
    public Page<MessageResponse.Simple> getMessagesRecentFirst(Long userId, Pageable pageable) {
        // 사용자의 대화 조회 (없으면 빈 페이지 반환)
        return findConversationByUserId(userId)
                .map(conversation -> {
                    Page<Message> messages = messageRepository.findByConversationIdOrderByCreatedDateDesc(conversation.getId(), pageable);
                    return messages.map(MessageResponse.Simple::from);
                })
                .orElse(Page.empty(pageable));
    }
    
    public List<MessageResponse.Simple> searchMessages(Long userId, String keyword) {
        // 사용자의 대화 조회 (없으면 빈 리스트 반환)
        return findConversationByUserId(userId)
                .map(conversation -> {
                    List<Message> messages = messageRepository.findByConversationIdAndContentContaining(conversation.getId(), keyword);
                    return messages.stream()
                            .map(MessageResponse.Simple::from)
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }
    
    @Transactional
    public void deleteMessage(Long messageId) {
        Message message = findMessageById(messageId);
        messageRepository.delete(message);
    }
    
    public long getMessageCount(Long userId) {
        // 사용자의 대화 조회 (없으면 0 반환)
        return findConversationByUserId(userId)
                .map(conversation -> messageRepository.countByConversationId(conversation.getId()))
                .orElse(0L);
    }
    
    public long getMessageCountByUserId(Long userId) {
        return getMessageCount(userId);
    }
    
    /**
     * 사용자의 대화 ID 조회 (없으면 null 반환)
     */
    public UUID getConversationId(Long userId) {
        return findConversationByUserId(userId)
                .map(Conversation::getId)
                .orElse(null);
    }
    
    private Message findMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new NotFoundException(ExceptionType.MESSAGE_NOT_FOUND, "ID: " + messageId));
    }
}
