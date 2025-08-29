package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatController 테스트")
class AiChatControllerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private AiChatController aiChatController;

    private UUID userId;
    private List<MessageResponse.ConversationInfo> mockConversationList;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        MessageResponse.ConversationInfo conversationInfo = new MessageResponse.ConversationInfo(
                UUID.randomUUID(),
                userId,
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now()
        );
        mockConversationList = List.of(conversationInfo);
    }

    @Test
    @DisplayName("시나리오 1: 대화 목록 조회 - getMyConversationList() 호출 시 messageService.getConversationListByUserId()가 올바른 userId로 호출되는지 검증")
    void getMyConversationList_ShouldCallMessageServiceWithCorrectUserId() {
        // Given
        when(messageService.getConversationListByUserId(userId)).thenReturn(mockConversationList);

        // When
        ResponseEntity<List<MessageResponse.ConversationInfo>> response = aiChatController.getMyConversationList(userId);

        // Then
        // messageService.getConversationListByUserId()가 컨트롤러에 전달된 userId로 호출되는지 검증
        verify(messageService).getConversationListByUserId(userId);
        
        // 응답 검증
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(mockConversationList);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).userId()).isEqualTo(userId);
    }
}
