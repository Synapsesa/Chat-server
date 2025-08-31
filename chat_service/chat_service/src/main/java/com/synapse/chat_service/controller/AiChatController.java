package com.synapse.chat_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.chat_service.service.MessageService;
import com.synapse.chat_service_api.dto.request.ChatMessageRequest;
import com.synapse.chat_service_api.dto.response.ChatHistoryResponse;
import com.synapse.chat_service_api.dto.response.MessageResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final MessageService messageService;

    @GetMapping("/conversation/list")
    public ResponseEntity<List<MessageResponse.ConversationInfo>> getMyConversationList(
        @AuthenticationPrincipal UUID userId
    ) {
        List<MessageResponse.ConversationInfo> response = messageService.getConversationListByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/recent")
    public ResponseEntity<ChatHistoryResponse> getMyAiChatHistoryRecentFirst(
        @AuthenticationPrincipal UUID userId,
        @Valid ChatMessageRequest request
    ) {
        ChatHistoryResponse response = messageService.getMessagesRecentFirst(userId, request.size(), request.cursor());
        return ResponseEntity.ok(response);
    }
}
