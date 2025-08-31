package com.synapse.chat_service.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.synapse.chat_service.common.util.RedisTypeConverter;
import com.synapse.chat_service_api.dto.session.AiChatInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisAiChatManager 테스트")
class RedisAiChatManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisKeyGenerator keyGenerator;

    @Mock
    private RedisTypeConverter typeConverter;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisAiChatManager redisAiChatManager;

    private AiChatInfo testAiChatInfo;
    private final String testUserId = "test-user-123";
    private final UUID testConversationId = UUID.randomUUID();
    private final String testAiChatKey = "ai:conversation:test-user-123";
    private final Duration AI_CHAT_EXPIRATION = Duration.ofDays(30);

    @BeforeEach
    void setUp() {
        testAiChatInfo = AiChatInfo.create(testUserId, testConversationId);

        // RedisTemplate operations mocking
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("AI 채팅 정보 조회 시 올바른 키 생성 및 Redis 호출 검증")
    void getAiChat_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo);
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When
        Optional<AiChatInfo> result = redisAiChatManager.getAiChat(testUserId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().userId()).isEqualTo(testUserId);
        assertThat(result.get().conversationId()).isEqualTo(testConversationId);

        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 정보 조회 시 데이터가 없는 경우 빈 Optional 반환")
    void getAiChat_WhenNoData_ShouldReturnEmptyOptional() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(null);
        when(typeConverter.convertValue(null, AiChatInfo.class)).thenReturn(null);

        // When
        Optional<AiChatInfo> result = redisAiChatManager.getAiChat(testUserId);

        // Then
        assertThat(result).isEmpty();

        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(typeConverter).convertValue(null, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 활동 시간 업데이트 시 올바른 키 생성 및 Redis 호출 검증")
    void updateAiChatActivity_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo);
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When
        redisAiChatManager.updateAiChatActivity(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get과 set 모두 호출되어야 함
        verify(redisTemplate, times(2)).opsForValue(); // get용 1번, set용 1번
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations).set(eq(testAiChatKey), any(AiChatInfo.class), eq(AI_CHAT_EXPIRATION));
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 활동 시간 업데이트 시 데이터가 없는 경우 Redis 호출하지 않음")
    void updateAiChatActivity_WhenNoData_ShouldNotCallRedisSet() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(null);
        when(typeConverter.convertValue(null, AiChatInfo.class)).thenReturn(null);

        // When
        redisAiChatManager.updateAiChatActivity(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get만 호출되고 set은 호출되지 않아야 함
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verify(typeConverter).convertValue(null, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 메시지 수 증가 시 올바른 키 생성 및 Redis 호출 검증")
    void incrementMessageCount_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo);
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When
        redisAiChatManager.incrementMessageCount(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get과 set 모두 호출되어야 함
        verify(redisTemplate, times(2)).opsForValue(); // get용 1번, set용 1번
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations).set(eq(testAiChatKey), any(AiChatInfo.class), eq(AI_CHAT_EXPIRATION));
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 메시지 수 증가 시 데이터가 없는 경우 Redis 호출하지 않음")
    void incrementMessageCount_WhenNoData_ShouldNotCallRedisSet() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(null);
        when(typeConverter.convertValue(null, AiChatInfo.class)).thenReturn(null);

        // When
        redisAiChatManager.incrementMessageCount(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get만 호출되고 set은 호출되지 않아야 함
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verify(typeConverter).convertValue(null, AiChatInfo.class);
    }

    @Test
    @DisplayName("AI 채팅 정보 삭제 시 올바른 키 생성 및 Redis 호출 검증")
    void deleteAiChat_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);

        // When
        redisAiChatManager.deleteAiChat(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate).delete(testAiChatKey);
    }

    @Test
    @DisplayName("UUID 기반 AI 채팅 세션 생성 시 올바른 키 생성 및 Redis 호출 검증")
    void createOrUpdateAiChatWithConversation_WhenNewChat_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(null); // 기존 채팅 없음
        when(typeConverter.convertValue(null, AiChatInfo.class)).thenReturn(null);

        // When
        AiChatInfo result = redisAiChatManager.createOrUpdateAiChatWithConversation(testUserId, testConversationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUserId);
        assertThat(result.conversationId()).isEqualTo(testConversationId);

        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate, times(2)).opsForValue(); // get용 1번, set용 1번
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations).set(eq(testAiChatKey), any(AiChatInfo.class), eq(AI_CHAT_EXPIRATION));
        verify(typeConverter).convertValue(null, AiChatInfo.class);
    }

    @Test
    @DisplayName("UUID 기반 AI 채팅 세션 업데이트 시 올바른 키 생성 및 Redis 호출 검증")
    void createOrUpdateAiChatWithConversation_WhenExistingChat_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        UUID newConversationId = UUID.randomUUID();
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo); // 기존 채팅 있음
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When
        AiChatInfo result = redisAiChatManager.createOrUpdateAiChatWithConversation(testUserId, newConversationId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUserId);
        assertThat(result.conversationId()).isEqualTo(newConversationId);

        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate, times(2)).opsForValue(); // get용 1번, set용 1번
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations).set(eq(testAiChatKey), any(AiChatInfo.class), eq(AI_CHAT_EXPIRATION));
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("Conversation ID 동기화 시 올바른 키 생성 및 Redis 호출 검증")
    void syncConversationId_WhenDifferentId_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        UUID newConversationId = UUID.randomUUID();
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo);
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When
        redisAiChatManager.syncConversationId(testUserId, newConversationId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증
        verify(redisTemplate, times(2)).opsForValue(); // get용 1번, set용 1번
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations).set(eq(testAiChatKey), any(AiChatInfo.class), eq(AI_CHAT_EXPIRATION));
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("Conversation ID 동기화 시 같은 ID인 경우 Redis set 호출하지 않음")
    void syncConversationId_WhenSameId_ShouldNotCallRedisSet() {
        // Given
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(testAiChatInfo);
        when(typeConverter.convertValue(testAiChatInfo, AiChatInfo.class)).thenReturn(testAiChatInfo);

        // When - 같은 conversationId로 동기화 시도
        redisAiChatManager.syncConversationId(testUserId, testConversationId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get만 호출되고 set은 호출되지 않아야 함
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verify(typeConverter).convertValue(testAiChatInfo, AiChatInfo.class);
    }

    @Test
    @DisplayName("Conversation ID 동기화 시 데이터가 없는 경우 Redis set 호출하지 않음")
    void syncConversationId_WhenNoData_ShouldNotCallRedisSet() {
        // Given
        UUID newConversationId = UUID.randomUUID();
        when(keyGenerator.generateAIConversationKey(testUserId)).thenReturn(testAiChatKey);
        when(valueOperations.get(testAiChatKey)).thenReturn(null);
        when(typeConverter.convertValue(null, AiChatInfo.class)).thenReturn(null);

        // When
        redisAiChatManager.syncConversationId(testUserId, newConversationId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateAIConversationKey(testUserId);

        // Redis 호출 검증 - get만 호출되고 set은 호출되지 않아야 함
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testAiChatKey);
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
        verify(typeConverter).convertValue(null, AiChatInfo.class);
    }
}
