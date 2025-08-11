package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.service.MessageService;
import com.synapse.chat_service.service.ai.AIModelType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageController 테스트")
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private MessageController messageController;

    private UUID userId;
    private MessageRequest.Chat chatMessage;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        chatMessage = new MessageRequest.Chat(
                AIModelType.GPT,
                "안녕하세요",
                "test-session-id",
                "test-message-id"
        );
    }

    @Test
    @DisplayName("시나리오 1: 채팅 메시지 핸들링 - @MessageMapping(\"/chat\")으로 메시지 수신 시 messageService.processAndRespondToMessage()가 올바른 파라미터로 호출되는지 검증")
    void handleChatMessage_ShouldCallMessageServiceWithCorrectParameters() {
        // Given
        when(authentication.getName()).thenReturn(userId.toString());

        // When
        messageController.handleChatMessage(chatMessage, authentication);

        // Then
        // messageService.processAndRespondToMessage()가 메시지 페이로드와 인증 정보(userId)로 호출되는지 검증
        verify(messageService).processAndRespondToMessage(userId, chatMessage);
        
        // Authentication에서 userId가 올바르게 추출되는지 검증
        verify(authentication).getName();
    }
}
