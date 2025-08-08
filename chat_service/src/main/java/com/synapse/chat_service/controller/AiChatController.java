package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.request.ChatMessageRequest;
import com.synapse.chat_service.dto.response.ChatHistoryResponse;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.service.MessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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
