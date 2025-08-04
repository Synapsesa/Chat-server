package com.synapse.chat_service.testutil;

import com.synapse.chat_service.domain.entity.ChatRoom;
import com.synapse.chat_service.domain.entity.ChatUsage;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.User;
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

    // ChatRoom 생성 메서드들
    public static ChatRoom createChatRoom(Long userId, String title) {
        return ChatRoom.builder()
                .userId(userId)
                .title(title)
                .build();
    }

    public static ChatRoom createDefaultChatRoom() {
        return createChatRoom(1L, "테스트 채팅방");
    }

    public static ChatRoom createChatRoomWithUserId(Long userId) {
        return createChatRoom(userId, "테스트 채팅방");
    }

    public static ChatRoom createChatRoomWithTitle(String title) {
        return createChatRoom(1L, title);
    }

    public static ChatRoom createChatRoomWithId(UUID id, Long userId, String title) {
        ChatRoom chatRoom = ChatRoom.builder()
                .userId(userId)
                .title(title)
                .build();
        setId(chatRoom, id);
        return chatRoom;
    }

    public static ChatRoom createChatRoomWithCreatedDate(Long userId, String title, LocalDateTime createdDate) {
        ChatRoom chatRoom = createChatRoom(userId, title);
        setCreatedDate(chatRoom, createdDate);
        return chatRoom;
    }

    // Message 생성 메서드들
    public static Message createMessage(ChatRoom chatRoom, SenderType senderType, String content) {
        return Message.builder()
                .chatRoom(chatRoom)
                .senderType(senderType)
                .content(content)
                .build();
    }

    public static Message createUserMessage(ChatRoom chatRoom, String content) {
        return createMessage(chatRoom, SenderType.USER, content);
    }

    public static Message createAssistantMessage(ChatRoom chatRoom, String content) {
        return createMessage(chatRoom, SenderType.ASSISTANT, content);
    }

    public static Message createDefaultUserMessage(ChatRoom chatRoom) {
        return createUserMessage(chatRoom, "사용자 테스트 메시지");
    }

    public static Message createDefaultAssistantMessage(ChatRoom chatRoom) {
        return createAssistantMessage(chatRoom, "AI 테스트 응답");
    }

    public static Message createMessageWithId(Long id, ChatRoom chatRoom, SenderType senderType, String content) {
        Message message = Message.builder()
                .chatRoom(chatRoom)
                .senderType(senderType)
                .content(content)
                .build();
        setId(message, id);
        return message;
    }

    public static Message createUserMessageWithId(Long id, ChatRoom chatRoom, String content) {
        return createMessageWithId(id, chatRoom, SenderType.USER, content);
    }

    public static Message createAssistantMessageWithId(Long id, ChatRoom chatRoom, String content) {
        return createMessageWithId(id, chatRoom, SenderType.ASSISTANT, content);
    }

    public static Message createMessageWithCreatedDate(ChatRoom chatRoom, SenderType senderType, String content, LocalDateTime createdDate) {
        Message message = createMessage(chatRoom, senderType, content);
        setCreatedDate(message, createdDate);
        return message;
    }

    // ChatUsage 생성 메서드들
    public static ChatUsage createChatUsage(Long userId, SubscriptionType subscriptionType, Integer messageLimit) {
        return ChatUsage.builder()
                .userId(userId)
                .subscriptionType(subscriptionType)
                .messageLimit(messageLimit)
                .build();
    }

    public static ChatUsage createFreeChatUsage(Long userId) {
        return createChatUsage(userId, SubscriptionType.FREE, 100);
    }

    public static ChatUsage createProChatUsage(Long userId) {
        return createChatUsage(userId, SubscriptionType.PRO, 1000);
    }

    public static ChatUsage createDefaultFreeChatUsage() {
        return createFreeChatUsage(1L);
    }

    public static ChatUsage createDefaultProChatUsage() {
        return createProChatUsage(1L);
    }

    // User 생성 메서드들
    public static User createUser(Long id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .build();
    }

    public static User createDefaultUser() {
        return createUser(1L, "testuser1", "testuser1@example.com");
    }

    public static User createUserWithId(Long id) {
        return createUser(id, "testuser" + id, "testuser" + id + "@example.com");
    }

    public static User createUserWithUsername(String username) {
        return createUser(1L, username, username + "@example.com");
    }

    public static User createUserWithEmail(String email) {
        return createUser(1L, "testuser", email);
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
        public static final String DEFAULT_CHAT_ROOM_TITLE = "테스트 채팅방";
        public static final String DEFAULT_USER_MESSAGE = "사용자 테스트 메시지";
        public static final String DEFAULT_ASSISTANT_MESSAGE = "AI 테스트 응답";
        public static final Integer FREE_MESSAGE_LIMIT = 100;
        public static final Integer PRO_MESSAGE_LIMIT = 1000;
    }
}