package com.synapse.chat_service.session;

import com.synapse.chat_service.common.util.RedisTypeConverter;
import com.synapse.chat_service.session.dto.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisSessionManager 테스트")
class RedisSessionManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisKeyGenerator keyGenerator;

    @Mock
    private RedisTypeConverter typeConverter;

    @Mock
    private SessionProperties sessionProperties;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private SetOperations<String, Object> setOperations;

    @InjectMocks
    private RedisSessionManager redisSessionManager;

    private SessionInfo testSessionInfo;
    private final String testSessionId = "test-session-123";
    private final String testUserId = "test-user-456";
    private final String testClientInfo = "Test Client Info";
    private final String testSessionKey = "session:test-session-123";
    private final String testUserSessionKey = "user:session:test-user-456";

    @BeforeEach
    void setUp() {
        testSessionInfo = SessionInfo.create(
                testSessionId,
                testUserId,
                testClientInfo
        );

        // RedisTemplate operations mocking
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        
        // SessionProperties mocking
        lenient().when(sessionProperties.maxSessionsPerUser()).thenReturn(3);
        lenient().when(sessionProperties.expirationHours()).thenReturn(24);
    }

    @Test
    @DisplayName("세션 조회 시 올바른 키 생성 및 Redis 호출 검증")
    void getSession_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionId)).thenReturn(testSessionKey);
        when(valueOperations.get(testSessionKey)).thenReturn(testSessionInfo);
        when(typeConverter.convertValue(testSessionInfo, SessionInfo.class)).thenReturn(testSessionInfo);

        // When
        SessionInfo result = redisSessionManager.getSession(testSessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo(testSessionId);
        
        // 키 생성 검증
        verify(keyGenerator).generateSessionKey(testSessionId);
        
        // Redis 호출 검증
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testSessionKey);
        verify(typeConverter).convertValue(testSessionInfo, SessionInfo.class);
    }

    @Test
    @DisplayName("세션 업데이트 시 올바른 키 생성 및 Redis 호출 검증")
    void updateSession_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionInfo.sessionId())).thenReturn(testSessionKey);

        // When
        redisSessionManager.updateSession(testSessionInfo);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateSessionKey(testSessionInfo.sessionId());
        
        // Redis 호출 검증
        verify(redisTemplate).opsForValue();
        verify(valueOperations).set(eq(testSessionKey), eq(testSessionInfo), eq(Duration.ofHours(24)));
    }

    @Test
    @DisplayName("활성 세션 수 조회 시 올바른 키 생성 및 Redis 호출 검증")
    void getActiveSessionCount_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateUserSessionKey(testUserId)).thenReturn(testUserSessionKey);
        when(setOperations.size(testUserSessionKey)).thenReturn(2L);

        // When
        int result = redisSessionManager.getActiveSessionCount(testUserId);

        // Then
        assertThat(result).isEqualTo(2);
        
        // 키 생성 검증
        verify(keyGenerator).generateUserSessionKey(testUserId);
        
        // Redis 호출 검증
        verify(redisTemplate).opsForSet();
        verify(setOperations).size(testUserSessionKey);
    }

    @Test
    @DisplayName("사용자별 세션 조회 시 올바른 키 생성 및 Redis 호출 검증")
    void getSessionByUserId_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateUserSessionKey(testUserId)).thenReturn(testUserSessionKey);
        when(keyGenerator.generateSessionKey(testSessionId)).thenReturn(testSessionKey);
        when(setOperations.members(testUserSessionKey)).thenReturn(Set.of(testSessionId));
        when(typeConverter.convertToString(testSessionId)).thenReturn(testSessionId);
        when(valueOperations.get(testSessionKey)).thenReturn(testSessionInfo);
        when(typeConverter.convertValue(testSessionInfo, SessionInfo.class)).thenReturn(testSessionInfo);

        // When
        SessionInfo result = redisSessionManager.getSessionByUserId(testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(testUserId);
        
        // 키 생성 검증
        verify(keyGenerator).generateUserSessionKey(testUserId);
        verify(keyGenerator).generateSessionKey(testSessionId);
        
        // Redis 호출 검증
        verify(redisTemplate).opsForSet(); // members 호출
        verify(setOperations).members(testUserSessionKey);
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testSessionKey);
    }

    @Test
    @DisplayName("세션 존재 확인 시 올바른 키 생성 및 Redis 호출 검증")
    void existsSession_ShouldGenerateCorrectKey_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionId)).thenReturn(testSessionKey);
        when(redisTemplate.hasKey(testSessionKey)).thenReturn(true);

        // When
        boolean result = redisSessionManager.existsSession(testSessionId);

        // Then
        assertThat(result).isTrue();
        
        // 키 생성 검증
        verify(keyGenerator).generateSessionKey(testSessionId);
        
        // Redis 호출 검증
        verify(redisTemplate).hasKey(testSessionKey);
    }

    @Test
    @DisplayName("최대 세션 수 초과 시 removeOldestSession 호출 검증")
    @SuppressWarnings("unchecked")
    void createSession_WhenMaxSessionsExceeded_ShouldCallRemoveOldestSession() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionInfo.sessionId())).thenReturn(testSessionKey);
        when(keyGenerator.generateUserSessionKey(testSessionInfo.userId())).thenReturn(testUserSessionKey);
        when(sessionProperties.maxSessionsPerUser()).thenReturn(2); // 최대 2개 세션
        when(setOperations.size(testUserSessionKey)).thenReturn(3L); // 현재 3개 세션 (초과)
        
        // removeOldestSession에서 사용할 기존 세션들 설정
        SessionInfo oldSession1 = SessionInfo.create("old-session-1", testUserId, "Old Client 1");
        SessionInfo oldSession2 = SessionInfo.create("old-session-2", testUserId, "Old Client 2");
        when(setOperations.members(testUserSessionKey)).thenReturn(Set.of("old-session-1", "old-session-2"));
        when(typeConverter.convertToString("old-session-1")).thenReturn("old-session-1");
        when(typeConverter.convertToString("old-session-2")).thenReturn("old-session-2");
        when(keyGenerator.generateSessionKey("old-session-1")).thenReturn("session:old-session-1");
        when(keyGenerator.generateSessionKey("old-session-2")).thenReturn("session:old-session-2");
        when(valueOperations.get("session:old-session-1")).thenReturn(oldSession1);
        when(valueOperations.get("session:old-session-2")).thenReturn(oldSession2);
        when(typeConverter.convertValue(oldSession1, SessionInfo.class)).thenReturn(oldSession1);
        when(typeConverter.convertValue(oldSession2, SessionInfo.class)).thenReturn(oldSession2);

        // RedisCallback 실행을 위한 Mock 설정
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        redisSessionManager.createSession(testSessionInfo);

        // Then
        // 키 생성 검증 - generateUserSessionKey는 총 4번 호출됨:
        // 1. createSession에서 직접 호출
        // 2. getActiveSessionCount 호출 시
        // 3. removeOldestSession -> getSessionsByUserId 호출 시
        // 4. removeOldestSession -> deleteSession 호출 시
        verify(keyGenerator).generateSessionKey(testSessionInfo.sessionId());
        verify(keyGenerator, times(4)).generateUserSessionKey(testSessionInfo.userId());
        
        // 활성 세션 수 조회 검증 (removeOldestSession 호출 여부 판단용)
        verify(redisTemplate, atLeastOnce()).opsForSet();
        verify(setOperations, atLeastOnce()).size(testUserSessionKey);
        
        // removeOldestSession 내부에서 호출되는 메서드들 검증
        verify(setOperations, atLeastOnce()).members(testUserSessionKey);
        
        // 세션 생성을 위한 Redis 트랜잭션 실행 검증
        verify(redisTemplate, atLeastOnce()).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("최대 세션 수 미만일 때 removeOldestSession 호출되지 않음 검증")
    @SuppressWarnings("unchecked")
    void createSession_WhenMaxSessionsNotExceeded_ShouldNotCallRemoveOldestSession() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionInfo.sessionId())).thenReturn(testSessionKey);
        when(keyGenerator.generateUserSessionKey(testSessionInfo.userId())).thenReturn(testUserSessionKey);
        when(sessionProperties.maxSessionsPerUser()).thenReturn(5); // 최대 5개 세션
        when(setOperations.size(testUserSessionKey)).thenReturn(2L); // 현재 2개 세션 (미만)
        
        // RedisCallback 실행을 위한 Mock 설정
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        redisSessionManager.createSession(testSessionInfo);

        // Then
        // 키 생성 검증 - generateUserSessionKey는 총 2번 호출됨:
        // 1. createSession에서 직접 호출
        // 2. getActiveSessionCount 호출 시
        verify(keyGenerator).generateSessionKey(testSessionInfo.sessionId());
        verify(keyGenerator, times(2)).generateUserSessionKey(testSessionInfo.userId());
        
        // 활성 세션 수 조회 검증
        verify(redisTemplate).opsForSet();
        verify(setOperations).size(testUserSessionKey);
        
        // removeOldestSession이 호출되지 않았음을 검증 (members 호출이 없음)
        verify(setOperations, never()).members(testUserSessionKey);
        
        // 세션 생성을 위한 Redis 트랜잭션 실행 검증
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("세션 삭제 시 올바른 키 생성 및 Redis 호출 검증")
    @SuppressWarnings("unchecked")
    void deleteSession_ShouldGenerateCorrectKeys_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateSessionKey(testSessionId)).thenReturn(testSessionKey);
        when(keyGenerator.generateUserSessionKey(testUserId)).thenReturn(testUserSessionKey);
        when(valueOperations.get(testSessionKey)).thenReturn(testSessionInfo);
        when(typeConverter.convertValue(testSessionInfo, SessionInfo.class)).thenReturn(testSessionInfo);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(null);

        // When
        redisSessionManager.deleteSession(testSessionId);

        // Then
        // 키 생성 검증
        verify(keyGenerator, times(2)).generateSessionKey(testSessionId); // getSession + deleteSession
        verify(keyGenerator).generateUserSessionKey(testUserId);
        
        // Redis 호출 검증
        verify(redisTemplate).opsForValue();
        verify(valueOperations).get(testSessionKey);
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    @DisplayName("모든 사용자 세션 삭제 시 올바른 키 생성 및 Redis 호출 검증")
    void deleteAllUserSessions_ShouldGenerateCorrectKeys_AndCallRedisTemplate() {
        // Given
        when(keyGenerator.generateUserSessionKey(testUserId)).thenReturn(testUserSessionKey);
        when(keyGenerator.generateSessionKey("session1")).thenReturn("session:session1");
        when(keyGenerator.generateSessionKey("session2")).thenReturn("session:session2");
        when(setOperations.members(testUserSessionKey)).thenReturn(Set.of("session1", "session2"));
        when(typeConverter.convertToString("session1")).thenReturn("session1");
        when(typeConverter.convertToString("session2")).thenReturn("session2");

        // When
        redisSessionManager.deleteAllUserSessions(testUserId);

        // Then
        // 키 생성 검증
        verify(keyGenerator).generateUserSessionKey(testUserId);
        verify(keyGenerator).generateSessionKey("session1");
        verify(keyGenerator).generateSessionKey("session2");
        
        // Redis 호출 검증
        verify(redisTemplate).opsForSet();
        verify(setOperations).members(testUserSessionKey);
        verify(redisTemplate, times(3)).delete(anyString()); // 개별 세션 2개 + 사용자 세션 Set 1개
    }
}
