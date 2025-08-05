package com.synapse.chat_service.session;

import com.synapse.chat_service.common.annotation.RedisOperation;
import com.synapse.chat_service.common.util.RedisTypeConverter;
import com.synapse.chat_service.session.dto.SessionInfo;
import com.synapse.chat_service.session.dto.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis를 사용한 WebSocket 세션 관리 서비스
 * 다중 기기 동시 접속을 지원하는 세션의 생성, 조회, 업데이트, 삭제를 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(SessionProperties.class)
public class RedisSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyGenerator keyGenerator;
    private final RedisTypeConverter typeConverter;
    private final SessionProperties sessionProperties;
    
    /**
     * 새로운 세션 생성 (다중 세션 지원, 트랜잭션 원자성 보장)
     */
    @RedisOperation("세션 생성")
    public void createSession(SessionInfo sessionInfo) {
        String sessionKey = keyGenerator.generateSessionKey(sessionInfo.sessionId());
        String userSessionKey = keyGenerator.generateUserSessionKey(sessionInfo.userId());
        
        // 최대 세션 수 확인 및 제한
        int currentSessionCount = getActiveSessionCount(sessionInfo.userId());
        if (currentSessionCount >= sessionProperties.maxSessionsPerUser()) {
            // 가장 오래된 세션 하나를 제거
            removeOldestSession(sessionInfo.userId());
            log.info("최대 세션 수 초과로 가장 오래된 세션 제거: userId={}", sessionInfo.userId());
        }
        
        // Redis 트랜잭션을 사용하여 원자성 보장
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            try {
                // 트랜잭션 시작
                connection.multi();
                
                // 1. 세션 정보 저장 (설정된 시간 TTL)
                byte[] sessionKeyBytes = sessionKey.getBytes();
                byte[] sessionValueBytes = typeConverter.convertToBytes(sessionInfo);
                connection.stringCommands().setEx(sessionKeyBytes, Duration.ofHours(sessionProperties.expirationHours()).toSeconds(), sessionValueBytes);

                // 2. 사용자별 세션 Set에 sessionId 추가
                byte[] userSessionKeyBytes = userSessionKey.getBytes();
                byte[] sessionIdBytes = sessionInfo.sessionId().getBytes();
                connection.setCommands().sAdd(userSessionKeyBytes, sessionIdBytes);

                // 3. 사용자 세션 Set TTL 설정 (설정된 시간)
                connection.keyCommands().expire(userSessionKeyBytes, Duration.ofHours(sessionProperties.expirationHours()).toSeconds());
                
                // 트랜잭션 실행
                connection.exec();
                
                log.info("세션 생성 완료 (트랜잭션): sessionId={}, userId={}, 총 세션 수={}", 
                        sessionInfo.sessionId(), sessionInfo.userId(), currentSessionCount + 1);
                
                return null;
                
            } catch (Exception e) {
                log.error("세션 생성 트랜잭션 실패: sessionId={}, userId={}", 
                         sessionInfo.sessionId(), sessionInfo.userId(), e);
                throw new RuntimeException("세션 생성 트랜잭션 실패", e);
            }
        });
    }
    
    /**
     * 세션 ID로 세션 조회
     */
    @RedisOperation("세션 조회")
    public SessionInfo getSession(String sessionId) {
        String sessionKey = keyGenerator.generateSessionKey(sessionId);
        Object rawValue = redisTemplate.opsForValue().get(sessionKey);
        SessionInfo sessionInfo = typeConverter.convertValue(rawValue, SessionInfo.class);
        
        log.debug("세션 조회: sessionId={}, found={}", sessionId, sessionInfo != null);
        return sessionInfo;
    }
    
    /**
     * 사용자 ID로 세션 정보 조회 (첫 번째 세션 반환)
     */
    @RedisOperation("사용자 세션 조회")
    public SessionInfo getSessionByUserId(String userId) {
        String userSessionKey = keyGenerator.generateUserSessionKey(userId);
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionKey);
        
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("사용자 세션 ID를 찾을 수 없음: userId={}", userId);
            return null;
        }
        
        // 첫 번째 세션 반환 (기존 호환성 유지)
        String sessionId = typeConverter.convertToString(sessionIds.iterator().next());
        return getSession(sessionId);
    }
    
    /**
     * 세션 정보 업데이트
     */
    @RedisOperation("세션 업데이트")
    public void updateSession(SessionInfo sessionInfo) {
        String sessionKey = keyGenerator.generateSessionKey(sessionInfo.sessionId());
        
        // 세션 정보 업데이트 (설정된 시간 TTL)
        redisTemplate.opsForValue().set(sessionKey, sessionInfo, Duration.ofHours(sessionProperties.expirationHours()));
        
        log.debug("세션 업데이트 완료: sessionId={}", sessionInfo.sessionId());
    }

    /**
     * 세션 삭제 (다중 세션 지원, 트랜잭션 원자성 보장)
     */
    @RedisOperation("세션 삭제")
    public void deleteSession(String sessionId) {
        SessionInfo sessionInfo = getSession(sessionId);
        if (sessionInfo != null) {
            String sessionKey = keyGenerator.generateSessionKey(sessionId);
            String userSessionKey = keyGenerator.generateUserSessionKey(sessionInfo.userId());
            
            // Redis 트랜잭션으로 원자성 보장
            redisTemplate.execute((RedisCallback<Object>) connection -> {
                try {
                    // 트랜잭션 시작
                    connection.multi();
                    
                    // 1. 개별 세션 삭제
                    byte[] sessionKeyBytes = sessionKey.getBytes();
                    connection.keyCommands().del(sessionKeyBytes);
                    
                    // 2. 사용자 세션 Set에서 해당 sessionId 제거
                    byte[] userSessionKeyBytes = userSessionKey.getBytes();
                    byte[] sessionIdBytes = sessionId.getBytes();
                    connection.setCommands().sRem(userSessionKeyBytes, sessionIdBytes);
                    
                    // 트랜잭션 실행
                    connection.exec();
                    
                    log.info("세션 삭제 완료 (트랜잭션): sessionId={}, userId={}", sessionId, sessionInfo.userId());
                    
                    return null;
                    
                } catch (Exception e) {
                    log.error("세션 삭제 트랜잭션 실패: sessionId={}, userId={}", 
                             sessionId, sessionInfo.userId(), e);
                    throw new RuntimeException("세션 삭제 트랜잭션 실패", e);
                }
            });
        }
    }
    
    /**
     * 사용자의 모든 세션 강제 삭제 (관리자 기능)
     */
    @RedisOperation(value = "사용자 모든 세션 삭제", rethrowException = false)
    public void deleteAllUserSessions(String userId) {
        String userSessionKey = keyGenerator.generateUserSessionKey(userId);
        
        // 1. 모든 세션 ID 조회
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionKey);
        
        if (sessionIds != null && !sessionIds.isEmpty()) {
            // 2. 각 세션 개별 삭제
            for (Object sessionIdObj : sessionIds) {
                String sessionId = typeConverter.convertToString(sessionIdObj);
                if (sessionId != null) {
                    String sessionKey = keyGenerator.generateSessionKey(sessionId);
                    redisTemplate.delete(sessionKey);
                    log.debug("세션 삭제: sessionId={}", sessionId);
                }
            }
        }
        
        // 3. 사용자-세션 Set 삭제
        redisTemplate.delete(userSessionKey);
        log.info("사용자 모든 세션 삭제 완료: userId={}, 삭제된 세션 수={}", 
                userId, sessionIds != null ? sessionIds.size() : 0);
    }
    
    /**
     * 세션 상태 변경
     */
    @RedisOperation("세션 상태 변경")
    public void changeSessionStatus(String sessionId, SessionStatus newStatus) {
        SessionInfo currentSession = getSession(sessionId);
        if (currentSession != null) {
            SessionInfo updatedSession = currentSession.changeStatus(newStatus);
            updateSession(updatedSession);
            log.info("세션 상태 변경: sessionId={}, status={}", sessionId, newStatus);
        }
    }
    
    /**
     * 세션 존재 여부 확인
     */
    @RedisOperation(value = "세션 존재 확인", returnDefaultOnError = true)
    public boolean existsSession(String sessionId) {
        String sessionKey = keyGenerator.generateSessionKey(sessionId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(sessionKey));
    }
    
    /**
     * 사용자 세션 존재 여부 확인 (다중 세션 지원)
     */
    @RedisOperation(value = "사용자 세션 존재 확인", returnDefaultOnError = true)
    public boolean existsSessionByUserId(String userId) {
        String userSessionKey = keyGenerator.generateUserSessionKey(userId);
        Long sessionCount = redisTemplate.opsForSet().size(userSessionKey);
        return sessionCount != null && sessionCount > 0;
    }
    
    /**
     * 사용자의 모든 세션 정보 조회
     */
    @RedisOperation("사용자 모든 세션 조회")
    public List<SessionInfo> getSessionsByUserId(String userId) {
        String userSessionKey = keyGenerator.generateUserSessionKey(userId);
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionKey);
        
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("사용자 세션을 찾을 수 없음: userId={}", userId);
            return List.of();
        }
        
        return sessionIds.stream()
                .map(sessionIdObj -> typeConverter.convertToString(sessionIdObj))
                .filter(sessionId -> sessionId != null)
                .map(this::getSession)
                .filter(sessionInfo -> sessionInfo != null)
                .collect(Collectors.toList());
    }
    
    /**
     * 사용자의 활성 세션 수 조회
     */
    @RedisOperation(value = "활성 세션 수 조회", returnDefaultOnError = true)
    public int getActiveSessionCount(String userId) {
        String userSessionKey = keyGenerator.generateUserSessionKey(userId);
        Long sessionCount = redisTemplate.opsForSet().size(userSessionKey);
        return sessionCount != null ? sessionCount.intValue() : 0;
    }
    
    /**
     * 가장 오래된 세션 제거 (최대 세션 수 초과 시 사용)
     */
    @RedisOperation(value = "가장 오래된 세션 제거", rethrowException = false)
    private void removeOldestSession(String userId) {
        List<SessionInfo> sessions = getSessionsByUserId(userId);
        if (!sessions.isEmpty()) {
            // 가장 오래된 세션 찾기 (연결 시간 기준)
            SessionInfo oldestSession = sessions.stream()
                    .min((s1, s2) -> s1.connectedAt().compareTo(s2.connectedAt()))
                    .orElse(null);
            
            if (oldestSession != null) {
                deleteSession(oldestSession.sessionId());
                log.info("가장 오래된 세션 제거: sessionId={}, userId={}", 
                        oldestSession.sessionId(), userId);
            }
        }
    }
}
