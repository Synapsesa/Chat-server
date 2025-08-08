package com.synapse.chat_service.testutil;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.ChatUsage;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.entity.enums.SubscriptionType;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 테스트 객체 생성을 위한 팩토리 클래스
 * 테스트 데이터 생성 로직을 중앙에서 관리하여 유지보수성을 향상시킵니다.
 */
public class TestObjectFactory {

    // Conversation 생성 메서드들
    public static Conversation createConversation(UUID userId) {
        return Conversation.builder()
                .userId(userId)
                .build();
    }

    public static Conversation createDefaultConversation() {
        return createConversation(UUID.randomUUID());

    }

    public static Conversation createConversationWithUserId(UUID userId) {
        return createConversation(userId);
    }

    public static Conversation createConversationWithId(UUID id, UUID userId) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .build();
        setId(conversation, id);
        return conversation;
    }

    public static Conversation createConversationWithCreatedDate(UUID userId, LocalDateTime createdDate) {
        Conversation conversation = createConversation(userId);
        setCreatedDate(conversation, createdDate);
        return conversation;
    }

    // Message 생성 메서드들
    public static Message createMessage(Conversation conversation, SenderType senderType, String content) {
        return Message.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(content)
                .build();
    }

    public static Message createUserMessage(Conversation conversation, String content) {
        return createMessage(conversation, SenderType.USER, content);
    }

    public static Message createAssistantMessage(Conversation conversation, String content) {
        return createMessage(conversation, SenderType.ASSISTANT, content);
    }

    public static Message createDefaultUserMessage(Conversation conversation) {
        return createUserMessage(conversation, "사용자 테스트 메시지");
    }

    public static Message createDefaultAssistantMessage(Conversation conversation) {
        return createAssistantMessage(conversation, "AI 테스트 응답");
    }

    public static Message createMessageWithId(Long id, Conversation conversation, SenderType senderType, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(content)
                .build();
        setId(message, id);
        return message;
    }

    public static Message createUserMessageWithId(Long id, Conversation conversation, String content) {
        return createMessageWithId(id, conversation, SenderType.USER, content);
    }

    public static Message createAssistantMessageWithId(Long id, Conversation conversation, String content) {
        return createMessageWithId(id, conversation, SenderType.ASSISTANT, content);
    }

    public static Message createMessageWithCreatedDate(Conversation conversation, SenderType senderType, String content, LocalDateTime createdDate) {
        Message message = createMessage(conversation, senderType, content);
        setCreatedDate(message, createdDate);
        return message;
    }

    // ChatUsage 생성 메서드들
    public static ChatUsage createChatUsage(UUID userId, SubscriptionType subscriptionType, Integer messageLimit) {
        return ChatUsage.builder()
                .userId(userId)
                .subscriptionType(subscriptionType)
                .messageLimit(messageLimit)
                .build();
    }

    public static ChatUsage createFreeChatUsage(UUID userId) {
        return createChatUsage(userId, SubscriptionType.FREE, 100);
    }

    public static ChatUsage createProChatUsage(UUID userId) {
        return createChatUsage(userId, SubscriptionType.PRO, 1000);
    }

    public static ChatUsage createDefaultFreeChatUsage() {
        return createFreeChatUsage(UUID.randomUUID());
    }

    public static ChatUsage createDefaultProChatUsage() {
        return createProChatUsage(UUID.randomUUID());
    }



    // Private 헬퍼 메서드들
    private static void setCreatedDate(Object entity, LocalDateTime createdDate) {
        try {
            Field createdDateField = entity.getClass().getSuperclass().getDeclaredField("createdDate");
            createdDateField.setAccessible(true);
            createdDateField.set(entity, createdDate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdDate", e);
        }
    }

    private static void setId(Object entity, Object id) {
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id", e);
        }
    }

    // 테스트용 상수들
    public static class TestConstants {
        public static final Long DEFAULT_USER_ID = 1L;
        public static final Long ANOTHER_USER_ID = 2L;
        public static final String DEFAULT_USER_MESSAGE = "사용자 테스트 메시지";
        public static final String DEFAULT_ASSISTANT_MESSAGE = "AI 테스트 응답";
        public static final Integer FREE_MESSAGE_LIMIT = 100;
        public static final Integer PRO_MESSAGE_LIMIT = 1000;
    }
}