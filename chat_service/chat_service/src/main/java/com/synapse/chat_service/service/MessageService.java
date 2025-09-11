package com.synapse.chat_service.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.chat_service.domain.entity.Conversation;
import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.domain.entity.enums.SenderType;
import com.synapse.chat_service.domain.repository.ConversationRepository;
import com.synapse.chat_service.domain.repository.MessageRepository;
import com.synapse.chat_service.service.ai.AIModelService;
import com.synapse.chat_service.service.ai.AIModelServiceFactory;
import com.synapse.chat_service.service.ai.AIModelType;
import com.synapse.chat_service.session.RedisAiChatManager;
import com.synapse.chat_service_api.dto.request.MessageRequest;
import com.synapse.chat_service_api.dto.response.ChatHistoryResponse;
import com.synapse.chat_service_api.dto.response.MessageResponse;
import com.synapse.chat_service_api.dto.response.PaginationDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final RedisAiChatManager redisAiChatManager;
    private final AIModelServiceFactory aiModelServiceFactory;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse.History createMessage(UUID userId, SenderType senderType, String content) {
        // 사용자의 대화가 존재하지 않으면 자동으로 생성
        Conversation conversation = getOrCreateConversation(userId);

        Message message = Message.builder()
                .conversation(conversation)
                .senderType(senderType)
                .content(content)
                .build();

        Message savedMessage = messageRepository.save(message);
        return MessageResponse.History.to(savedMessage.getId(), savedMessage.getConversation().getId(), savedMessage.getSenderType().toString(), savedMessage.getContent(), savedMessage.getCreatedDate());
    }

    /**
     * 사용자의 대화를 조회하거나 없으면 새로 생성
     * Redis의 AiChatInfo와 DB의 Conversation 간 일관성을 보장
     */
    public Conversation getOrCreateConversation(UUID userId) {
        return findConversationByUserId(userId)
                .map(conversation -> {
                    // 기존 대화가 있으면 Redis 정보 동기화
                    redisAiChatManager.syncConversationId(userId.toString(), conversation.getId());
                    return conversation;
                })
                .orElseGet(() -> {
                    // 새로운 대화 생성
                    Conversation newConversation = Conversation.builder()
                            .userId(userId)
                            .build();

                    Conversation savedConversation = conversationRepository.save(newConversation);

                    // Redis에 새로운 대화 정보 저장
                    redisAiChatManager.createOrUpdateAiChatWithConversation(
                            userId.toString(),
                            savedConversation.getId());

                    return savedConversation;
                });
    }

    private Optional<Conversation> findConversationByUserId(UUID userId) {
        return conversationRepository.findByUserId(userId);
    }

    private Optional<List<Conversation>> findConversationListByUserId(UUID userId) {
        return conversationRepository.findByUserIdOrderByCreatedDateDesc(userId);
    }

    /**
     * 추후 쿼리 최적화 필요 -> 조회시점에 불필요한 쿼리를 날리지 않는지 확인 필요
     * 
     * @param userId
     * @return
     */
    public List<MessageResponse.ConversationInfo> getConversationListByUserId(UUID userId) {
        // 사용자의 대화방 정보 조회 (없으면 null 반환)
        return findConversationListByUserId(userId)
                .map(conversationList -> conversationList.stream()
                        .map(conversation -> MessageResponse.ConversationInfo.to(conversation.getId(), conversation.getUserId(), conversation.getCreatedDate(), conversation.getUpdatedDate()))
                        .toList())
                .orElse(List.of());
    }

    /**
     * 커서 방식으로 쿼리 최적화 필요 -> 많은 대화 중에 메시지가 많을 수 있음
     * 
     * @param userId
     * @param size
     * @param cursor
     * @return
     */
    public ChatHistoryResponse getMessagesRecentFirst(UUID userId, Integer size, String cursor) {
        int queryLimit = size + 1;

        Long cursorId = cursor != null && !cursor.isEmpty() ? parseCursor(cursor) : null;

        List<MessageResponse.History> messages = messageRepository.findByUserIdWithCursorDesc(
                userId, cursorId, queryLimit);

        // 다음 페이지 존재 여부 확인
        boolean hasNext = messages.size() > size;
        if (hasNext) {
            messages = messages.subList(0, size); // 실제 반환할 데이터만 유지
        }

        // 다음 커서 생성 (마지막 메시지의 ID)
        String nextCursor = hasNext && !messages.isEmpty()
                ? generateCursor(messages.get(messages.size() - 1).id())
                : null;

        // 페이지네이션 정보 생성
        PaginationDto pagination = PaginationDto.of(nextCursor, hasNext, size);

        return ChatHistoryResponse.of(messages, pagination);
    }

    /**
     * ChatController로부터 위임받은 메시지 처리 및 응답 로직
     * 사용자 메시지 저장, AI 호출, 응답 저장, 클라이언트 전송을 통합 처리
     */
    @Transactional
    public void processAndRespondToMessage(UUID userId, MessageRequest.Chat chatMessage) {
        try {
            // 사용자 메시지 저장
            createMessage(userId, SenderType.USER, chatMessage.prompt());
            log.info("사용자 메시지 저장 완료 - 사용자ID: {}, 세션ID: {}", userId, chatMessage.sessionId());
        } catch (Exception e) {
            log.error("메시지 처리 중 최상위 오류 발생 - 사용자ID: {}", userId, e);
            sendErrorResponse(userId, chatMessage, "메시지 처리 중 서버 오류가 발생했습니다.");
        }

        // AI 서비스 호출
        AIModelService modelService = aiModelServiceFactory.getService(AIModelType.valueOf(chatMessage.modelType()));
        modelService.generateResponse(chatMessage.prompt())
                .thenApply(aiResponse -> {
                    createMessage(userId, SenderType.ASSISTANT, aiResponse);
                    log.info("AI 응답 저장 완료 - 사용자ID: {}", userId);
                    return MessageResponse.Chat.success(
                            aiResponse, chatMessage.modelType(), chatMessage.sessionId(), chatMessage.messageId());
                })
                .thenAccept(aiResponse -> {
                    sendResponse(userId, aiResponse);
                    log.info("AI 응답 전송 완료 - 사용자ID: {}", userId);
                })
                .exceptionally(throwable -> {
                    log.error("AI 응답 처리 파이프라인 실패 - 사용자ID: {}, 원인: {}", userId, throwable.getMessage(), throwable);
                    sendErrorResponse(userId, chatMessage, "AI 응답을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
                    return null;
                });
    }

    /**
     * 에러 응답을 클라이언트에게 전송
     */
    public void sendErrorResponse(UUID userId, MessageRequest.Chat chatMessage, String errorMessage) {
        MessageResponse.Chat errorResponse = MessageResponse.Chat.error(
                chatMessage.modelType(),
                chatMessage.sessionId(),
                chatMessage.messageId(),
                errorMessage);

        sendResponse(userId, errorResponse);

        log.warn("에러 응답 전송 - 세션: {}, 메시지ID: {}, 에러: {}",
                chatMessage.sessionId(), chatMessage.messageId(), errorMessage);
    }

    /**
     * 응답을 클라이언트에게 전송
     */
    private void sendResponse(UUID userId, MessageResponse.Chat response) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/response",
                response);
    }

    private Long parseCursor(String cursor) {
        String decodedCursor = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
        return Long.parseLong(decodedCursor);
    }

    private String generateCursor(Long messageId) {
        return Base64.getEncoder().encodeToString(messageId.toString().getBytes(StandardCharsets.UTF_8));
    }
}
