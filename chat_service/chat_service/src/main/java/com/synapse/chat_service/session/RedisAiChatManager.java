package com.synapse.chat_service.session;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.synapse.chat_service.common.annotation.RedisOperation;
import com.synapse.chat_service.common.util.RedisTypeConverter;
import com.synapse.chat_service_api.dto.session.AiChatInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisAiChatManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyGenerator keyGenerator;
    private final RedisTypeConverter typeConverter;

    // AI 채팅 정보는 30일간 유지 (사용자가 다시 접속할 수 있도록)
    private static final Duration AI_CHAT_EXPIRATION = Duration.ofDays(30);

    /**
     * AI 채팅 정보 조회
     */
    @RedisOperation(value = "AI 채팅 정보 조회", returnDefaultOnError = true)
    public Optional<AiChatInfo> getAiChat(String userId) {
        String key = keyGenerator.generateAIConversationKey(userId);
        Object rawValue = redisTemplate.opsForValue().get(key);
        AiChatInfo aiChat = typeConverter.convertValue(rawValue, AiChatInfo.class);
        return Optional.ofNullable(aiChat);
    }

    /**
     * AI 채팅 활동 시간 업데이트
     */
    @RedisOperation(value = "AI 채팅 활동 시간 업데이트", rethrowException = false)
    public void updateAiChatActivity(String userId) {
        String key = keyGenerator.generateAIConversationKey(userId);
        Object rawValue = redisTemplate.opsForValue().get(key);
        AiChatInfo aiChat = typeConverter.convertValue(rawValue, AiChatInfo.class);

        if (aiChat != null) {
            AiChatInfo updatedChat = aiChat.updateLastActivity();
            redisTemplate.opsForValue().set(key, updatedChat, AI_CHAT_EXPIRATION);

            log.debug("AI 채팅 활동 시간 업데이트: userId={}", userId);
        }
    }

    /**
     * AI 채팅 메시지 수 증가
     */
    @RedisOperation(value = "AI 채팅 메시지 수 증가", rethrowException = false)
    public void incrementMessageCount(String userId) {
        String key = keyGenerator.generateAIConversationKey(userId);
        Object rawValue = redisTemplate.opsForValue().get(key);
        AiChatInfo aiChat = typeConverter.convertValue(rawValue, AiChatInfo.class);

        if (aiChat != null) {
            AiChatInfo updatedChat = aiChat.incrementMessageCount();
            redisTemplate.opsForValue().set(key, updatedChat, AI_CHAT_EXPIRATION);

            log.debug("AI 채팅 메시지 수 증가: userId={}, count={}",
                    userId, updatedChat.messageCount());
        }
    }

    /**
     * AI 채팅 정보 삭제 (사용자 탈퇴 등의 경우)
     */
    @RedisOperation(value = "AI 채팅 정보 삭제", rethrowException = false)
    public void deleteAiChat(String userId) {
        String key = keyGenerator.generateAIConversationKey(userId);
        redisTemplate.delete(key);

        log.info("AI 채팅 정보 삭제: userId={}", userId);
    }

    /**
     * 실제 Conversation UUID를 사용하여 AI 채팅 세션 생성 또는 업데이트
     * Redis와 DB 간의 일관성을 보장합니다.
     */
    @RedisOperation("UUID 기반 AI 채팅 세션 생성/업데이트")
    public AiChatInfo createOrUpdateAiChatWithConversation(String userId, UUID conversationId) {
        String key = keyGenerator.generateAIConversationKey(userId);

        // 1. 기존 AI 채팅 정보 조회
        Object rawValue = redisTemplate.opsForValue().get(key);
        AiChatInfo existingChat = typeConverter.convertValue(rawValue, AiChatInfo.class);

        if (existingChat != null) {
            // 기존 채팅이 있으면 conversationId 업데이트 및 활동 시간 갱신
            AiChatInfo updatedChat = new AiChatInfo(
                    existingChat.userId(),
                    conversationId, // 실제 DB의 UUID로 업데이트
                    existingChat.createdAt(),
                    java.time.LocalDateTime.now(), // 활동 시간 갱신
                    existingChat.messageCount());
            redisTemplate.opsForValue().set(key, updatedChat, AI_CHAT_EXPIRATION);

            log.debug("AI 채팅 정보 업데이트: userId={}, conversationId={}",
                    userId, conversationId);
            return updatedChat;
        }

        // 2. 새로운 AI 채팅 정보 생성
        AiChatInfo newChat = AiChatInfo.create(userId, conversationId);
        redisTemplate.opsForValue().set(key, newChat, AI_CHAT_EXPIRATION);

        log.info("새로운 AI 채팅 정보 생성: userId={}, conversationId={}",
                userId, conversationId);
        return newChat;
    }

    /**
     * 기존 Redis 정보의 conversationId를 실제 DB UUID와 동기화
     */
    @RedisOperation(value = "Conversation ID 동기화", rethrowException = false)
    public void syncConversationId(String userId, UUID conversationId) {
        String key = keyGenerator.generateAIConversationKey(userId);
        Object rawValue = redisTemplate.opsForValue().get(key);
        AiChatInfo aiChat = typeConverter.convertValue(rawValue, AiChatInfo.class);

        if (aiChat != null && !conversationId.equals(aiChat.conversationId())) {
            // conversationId가 다르면 동기화
            AiChatInfo syncedChat = new AiChatInfo(
                    aiChat.userId(),
                    conversationId, // 실제 DB의 UUID로 동기화
                    aiChat.createdAt(),
                    java.time.LocalDateTime.now(), // 활동 시간 갱신
                    aiChat.messageCount());
            redisTemplate.opsForValue().set(key, syncedChat, AI_CHAT_EXPIRATION);

            log.info("Conversation ID 동기화: userId={}, oldId={}, newId={}",
                    userId, aiChat.conversationId(), conversationId);
        }
    }
}
