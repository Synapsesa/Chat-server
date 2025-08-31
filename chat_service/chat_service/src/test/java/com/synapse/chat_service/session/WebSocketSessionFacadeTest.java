package com.synapse.chat_service.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.synapse.chat_service_api.dto.session.SessionInfo;
import com.synapse.chat_service_api.dto.session.SessionStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketSessionFacade 테스트")
class WebSocketSessionFacadeTest {

    @Mock
    private RedisSessionManager sessionManager;

    @Mock
    private RedisAiChatManager aiChatManager;

    @InjectMocks
    private WebSocketSessionFacade webSocketSessionFacade;

    private String testSessionId;
    private String testUserId;
    private String testClientInfo;
    private SessionInfo testSessionInfo;

    @BeforeEach
    void setUp() {
        testSessionId = "test-session-123";
        testUserId = "test-user-456";
        testClientInfo = "Chrome/120.0 Windows";

        testSessionInfo = new SessionInfo(
                testSessionId,
                testUserId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                SessionStatus.CONNECTED,
                testClientInfo);
    }

    @Test
    @DisplayName("시나리오 1: 사용자 연결 처리 - handleUserConnection() 호출 시 올바른 인자로 메서드들이 호출되는지 검증")
    void handleUserConnection_ShouldCallCorrectMethods_WithCorrectArguments() {
        // When
        SessionInfo result = webSocketSessionFacade.handleUserConnection(testSessionId, testUserId, testClientInfo);

        // Then
        // 1. sessionManager.createSession()이 올바른 SessionInfo로 호출되는지 검증
        verify(sessionManager, times(1)).createSession(any(SessionInfo.class));

        // createSession에 전달된 SessionInfo의 내용 검증
        verify(sessionManager).createSession(argThat(sessionInfo -> sessionInfo.sessionId().equals(testSessionId) &&
                sessionInfo.userId().equals(testUserId) &&
                sessionInfo.clientInfo().equals(testClientInfo) &&
                sessionInfo.status() == SessionStatus.CONNECTED));

        // 2. aiChatManager.updateAiChatActivity()가 올바른 userId로 호출되는지 검증
        verify(aiChatManager, times(1)).updateAiChatActivity(eq(testUserId));

        // 3. 반환된 SessionInfo 검증
        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo(testSessionId);
        assertThat(result.userId()).isEqualTo(testUserId);
        assertThat(result.clientInfo()).isEqualTo(testClientInfo);
        assertThat(result.status()).isEqualTo(SessionStatus.CONNECTED);
    }

    @Test
    @DisplayName("시나리오 2: 사용자 연결 해제 처리 - 세션 정보가 있을 경우 올바른 순서로 메서드들이 호출되는지 검증")
    void handleUserDisconnection_WithExistingSession_ShouldCallCorrectMethods() {
        // Given
        when(sessionManager.getSession(testSessionId)).thenReturn(testSessionInfo);

        // When
        webSocketSessionFacade.handleUserDisconnection(testSessionId);

        // Then
        // 1. sessionManager.getSession()이 올바른 sessionId로 호출되는지 검증
        verify(sessionManager, times(1)).getSession(eq(testSessionId));

        // 2. aiChatManager.updateAiChatActivity()가 올바른 userId로 호출되는지 검증
        verify(aiChatManager, times(1)).updateAiChatActivity(eq(testUserId));

        // 3. sessionManager.deleteSession()이 올바른 sessionId로 호출되는지 검증
        verify(sessionManager, times(1)).deleteSession(eq(testSessionId));

        // 4. 메서드 호출 순서 검증
        var inOrder = inOrder(sessionManager, aiChatManager);
        inOrder.verify(sessionManager).getSession(testSessionId);
        inOrder.verify(aiChatManager).updateAiChatActivity(testUserId);
        inOrder.verify(sessionManager).deleteSession(testSessionId);
    }

    @Test
    @DisplayName("시나리오 2-1: 사용자 연결 해제 처리 - 세션 정보가 없을 경우 deleteSession이 호출되지 않는지 검증")
    void handleUserDisconnection_WithNoSession_ShouldNotCallDeleteSession() {
        // Given
        when(sessionManager.getSession(testSessionId)).thenReturn(null);

        // When
        webSocketSessionFacade.handleUserDisconnection(testSessionId);

        // Then
        // 1. sessionManager.getSession()이 호출되는지 검증
        verify(sessionManager, times(1)).getSession(eq(testSessionId));

        // 2. aiChatManager.updateAiChatActivity()가 호출되지 않는지 검증
        verify(aiChatManager, never()).updateAiChatActivity(any());

        // 3. sessionManager.deleteSession()이 호출되지 않는지 검증
        verify(sessionManager, never()).deleteSession(any());
    }

    @Test
    @DisplayName("메시지 활동 처리 - handleMessageActivity() 호출 시 올바른 메서드들이 호출되는지 검증")
    void handleMessageActivity_ShouldCallCorrectMethods() {
        // When
        webSocketSessionFacade.handleMessageActivity(testUserId);

        // Then
        // 1. aiChatManager.incrementMessageCount()가 올바른 userId로 호출되는지 검증
        verify(aiChatManager, times(1)).incrementMessageCount(eq(testUserId));

        // 2. aiChatManager.updateAiChatActivity()가 올바른 userId로 호출되는지 검증
        verify(aiChatManager, times(1)).updateAiChatActivity(eq(testUserId));

        // 3. 메서드 호출 순서 검증
        var inOrder = inOrder(aiChatManager);
        inOrder.verify(aiChatManager).incrementMessageCount(testUserId);
        inOrder.verify(aiChatManager).updateAiChatActivity(testUserId);
    }

    @Test
    @DisplayName("세션 활동 업데이트 - updateSessionActivity() 호출 시 올바른 메서드들이 호출되는지 검증")
    void updateSessionActivity_WithExistingSession_ShouldCallCorrectMethods() {
        // Given
        when(sessionManager.getSession(testSessionId)).thenReturn(testSessionInfo);

        // When
        webSocketSessionFacade.updateSessionActivity(testSessionId);

        // Then
        // 1. sessionManager.getSession()이 올바른 sessionId로 호출되는지 검증
        verify(sessionManager, times(1)).getSession(eq(testSessionId));

        // 2. sessionManager.updateSession()이 호출되는지 검증
        verify(sessionManager, times(1)).updateSession(any(SessionInfo.class));

        // 3. updateSession에 전달된 SessionInfo가 올바른 필드를 가지는지 검증
        verify(sessionManager).updateSession(argThat(sessionInfo -> sessionInfo.sessionId().equals(testSessionId) &&
                sessionInfo.userId().equals(testUserId) &&
                sessionInfo.clientInfo().equals(testClientInfo) &&
                sessionInfo.status() == SessionStatus.CONNECTED &&
                !sessionInfo.lastActivityAt().isBefore(testSessionInfo.lastActivityAt())));
    }

    @Test
    @DisplayName("세션 활동 업데이트 - 세션이 없을 경우 updateSession이 호출되지 않는지 검증")
    void updateSessionActivity_WithNoSession_ShouldNotCallUpdateSession() {
        // Given
        when(sessionManager.getSession(testSessionId)).thenReturn(null);

        // When
        webSocketSessionFacade.updateSessionActivity(testSessionId);

        // Then
        // 1. sessionManager.getSession()이 호출되는지 검증
        verify(sessionManager, times(1)).getSession(eq(testSessionId));

        // 2. sessionManager.updateSession()이 호출되지 않는지 검증
        verify(sessionManager, never()).updateSession(any());
    }

    @Test
    @DisplayName("사용자 모든 세션 강제 삭제 - forceDeleteAllUserSessions() 호출 시 올바른 메서드가 호출되는지 검증")
    void forceDeleteAllUserSessions_ShouldCallCorrectMethod() {
        // When
        webSocketSessionFacade.forceDeleteAllUserSessions(testUserId);

        // Then
        // sessionManager.deleteAllUserSessions()가 올바른 userId로 호출되는지 검증
        verify(sessionManager, times(1)).deleteAllUserSessions(eq(testUserId));
    }
}
