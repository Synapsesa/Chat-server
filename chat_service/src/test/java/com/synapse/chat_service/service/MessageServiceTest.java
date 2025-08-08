package com.synapse.chat_service.service;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.ConversationRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.service.ai.AIModelService;
import com.synapse.chat_service.service.ai.AIModelServiceFactory;
import com.synapse.chat_service.service.ai.AIModelType;
import com.synapse.chat_service.session.RedisAiChatManager;
import com.synapse.chat_service.testutil.TestObjectFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 테스트")
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private RedisAiChatManager redisAiChatManager;
    
    @Mock
    private AIModelServiceFactory aiModelServiceFactory;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private AIModelService aiModelService;
    
    @InjectMocks
    private MessageService messageService;
    
    private UUID userId;
    private String userMessage;
    private String aiResponse;
    private Conversation conversation;
    private Message savedUserMessage;
    private Message savedAiMessage;
    private MessageRequest.Chat chatRequest;
    
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userMessage = TestObjectFactory.TestConstants.DEFAULT_USER_MESSAGE;
        aiResponse = TestObjectFactory.TestConstants.DEFAULT_ASSISTANT_MESSAGE;
        
        conversation = TestObjectFactory.createConversation(userId);
        savedUserMessage = TestObjectFactory.createMessage(conversation, SenderType.USER, userMessage);
        savedAiMessage = TestObjectFactory.createMessage(conversation, SenderType.ASSISTANT, aiResponse);
        
        chatRequest = new MessageRequest.Chat(
            AIModelType.GPT,
            userMessage,
            "test-session-id",
            "test-message-id"
        );
    }
    
    @Test
    @DisplayName("시나리오 1: 신규 사용자 메시지 생성")
    void createMessage_NewUser_ShouldCreateConversationAndMessage() {
        // Given: 특정 userId로 처음 메시지를 보내는 상황
        when(conversationRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(savedUserMessage);
        
        // When: createMessage() 호출
        MessageResponse.History result = messageService.createMessage(userId, SenderType.USER, userMessage);
        
        // Then: 검증
        // conversationRepository.findByUserId()가 Optional.empty()를 반환하는지 검증
        verify(conversationRepository).findByUserId(userId);
        
        // 새로운 Conversation 객체가 생성되어 conversationRepository.save()로 저장되는지 검증
        ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(conversationCaptor.capture());
        Conversation capturedConversation = conversationCaptor.getValue();
        assertThat(capturedConversation.getUserId()).isEqualTo(userId);
        
        // messageRepository.save()가 올바른 Message 객체로 호출되는지 확인
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        Message capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getSenderType()).isEqualTo(SenderType.USER);
        assertThat(capturedMessage.getContent()).isEqualTo(userMessage);
        
        // redisAiChatManager.createOrUpdateAiChatWithConversation()이 호출되는지 검증
        verify(redisAiChatManager).createOrUpdateAiChatWithConversation(
            userId.toString(), 
            conversation.getId()
        );
        
        // 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.senderType()).isEqualTo(SenderType.USER);
        assertThat(result.content()).isEqualTo(userMessage);
    }
    
    @Test
    @DisplayName("시나리오 2: 기존 사용자 메시지 생성")
    void createMessage_ExistingUser_ShouldNotCreateConversation() {
        // Given: 기존에 대화가 있는 userId
        when(conversationRepository.findByUserId(userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedUserMessage);
        
        // When: createMessage() 호출
        MessageResponse.History result = messageService.createMessage(userId, SenderType.USER, userMessage);
        
        // Then: 검증
        // conversationRepository.findByUserId()가 Optional<Conversation>을 반환하는지 검증
        verify(conversationRepository).findByUserId(userId);
        
        // conversationRepository.save()가 호출되지 않는지 검증
        verify(conversationRepository, never()).save(any(Conversation.class));
        
        // messageRepository.save()가 호출되는지 확인
        verify(messageRepository).save(any(Message.class));
        
        // Redis 동기화가 호출되는지 검증
        verify(redisAiChatManager).syncConversationId(userId.toString(), conversation.getId());
        
        // 결과 검증
        assertThat(result).isNotNull();
        assertThat(result.senderType()).isEqualTo(SenderType.USER);
        assertThat(result.content()).isEqualTo(userMessage);
    }
    
    @Test
    @DisplayName("시나리오 3: AI 응답 처리 성공 (Happy Path)")
    void processAndRespondToMessage_Success_ShouldProcessCorrectly() {
        // Given: 유효한 MessageRequest.Chat 객체
        when(conversationRepository.findByUserId(userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class)))
            .thenReturn(savedUserMessage)
            .thenReturn(savedAiMessage);
        when(aiModelServiceFactory.getService(AIModelType.GPT)).thenReturn(aiModelService);
        when(aiModelService.generateResponse(userMessage))
            .thenReturn(CompletableFuture.completedFuture(aiResponse));
        
        // When: processAndRespondToMessage() 호출
        messageService.processAndRespondToMessage(userId, chatRequest);
        
        // 비동기 처리 완료를 위한 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: 검증
        // 사용자 메시지 저장을 위해 createMessage(userId, SenderType.USER, ...)가 호출되는지 검증
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository, times(2)).save(messageCaptor.capture());
        
        // 첫 번째 저장은 사용자 메시지
        Message firstSavedMessage = messageCaptor.getAllValues().get(0);
        assertThat(firstSavedMessage.getSenderType()).isEqualTo(SenderType.USER);
        assertThat(firstSavedMessage.getContent()).isEqualTo(userMessage);
        
        // aiModelServiceFactory.getService()가 올바른 AI 모델 타입으로 호출되는지 검증
        verify(aiModelServiceFactory).getService(AIModelType.GPT);
        
        // AIModelService의 generateResponse()가 호출되는지 검증
        verify(aiModelService).generateResponse(userMessage);
        
        // AI 응답 저장을 위해 createMessage(userId, SenderType.ASSISTANT, ...)가 호출되는지 검증
        Message secondSavedMessage = messageCaptor.getAllValues().get(1);
        assertThat(secondSavedMessage.getSenderType()).isEqualTo(SenderType.ASSISTANT);
        assertThat(secondSavedMessage.getContent()).isEqualTo(aiResponse);
        
        // messagingTemplate.convertAndSendToUser()가 성공(SUCCESS) 상태의 MessageResponse.Chat 객체와 함께 호출되는지 검증
        ArgumentCaptor<MessageResponse.Chat> responseCaptor = ArgumentCaptor.forClass(MessageResponse.Chat.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq("/queue/response"),
            responseCaptor.capture()
        );
        
        MessageResponse.Chat capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.status()).isEqualTo(MessageResponse.ResponseStatus.SUCCESS);
        assertThat(capturedResponse.response()).isEqualTo(aiResponse);
        assertThat(capturedResponse.modelType()).isEqualTo(AIModelType.GPT);
        assertThat(capturedResponse.sessionId()).isEqualTo("test-session-id");
        assertThat(capturedResponse.messageId()).isEqualTo("test-message-id");
    }
    
    @Test
    @DisplayName("시나리오 4: AI 응답 처리 실패 (AI 모델 오류)")
    void processAndRespondToMessage_AIModelFailure_ShouldSendErrorResponse() {
        // Given: 유효한 MessageRequest.Chat 객체, AI 모델 서비스가 실패하도록 설정
        when(conversationRepository.findByUserId(userId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(savedUserMessage);
        when(aiModelServiceFactory.getService(AIModelType.GPT)).thenReturn(aiModelService);
        
        // AI 모델 서비스가 실패하도록 설정
        CompletableFuture<String> failedFuture = CompletableFuture.failedFuture(
            new RuntimeException("AI 모델 응답 생성 중 오류가 발생했습니다.")
        );
        when(aiModelService.generateResponse(userMessage)).thenReturn(failedFuture);
        
        // When: processAndRespondToMessage() 호출
        messageService.processAndRespondToMessage(userId, chatRequest);
        
        // 비동기 처리 완료를 위한 대기
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then: 검증
        // 사용자 메시지는 저장되어야 함
        verify(messageRepository, times(1)).save(any(Message.class));
        
        // messagingTemplate.convertAndSendToUser()가 에러(ERROR) 상태의 MessageResponse.Chat 객체와 함께 호출되는지 검증
        ArgumentCaptor<MessageResponse.Chat> responseCaptor = ArgumentCaptor.forClass(MessageResponse.Chat.class);
        verify(messagingTemplate).convertAndSendToUser(
            eq(userId.toString()),
            eq("/queue/response"),
            responseCaptor.capture()
        );
        
        MessageResponse.Chat capturedResponse = responseCaptor.getValue();
        assertThat(capturedResponse.status()).isEqualTo(MessageResponse.ResponseStatus.ERROR);
        assertThat(capturedResponse.response()).isNull();
        assertThat(capturedResponse.errorMessage()).isNotNull();
        assertThat(capturedResponse.modelType()).isEqualTo(AIModelType.GPT);
        assertThat(capturedResponse.sessionId()).isEqualTo("test-session-id");
        assertThat(capturedResponse.messageId()).isEqualTo("test-message-id");
        
        // AI 응답 메시지가 DB에 저장되지 않는지 확인 (사용자 메시지만 1번 저장됨)
        verify(messageRepository, times(1)).save(any(Message.class));
    }
}