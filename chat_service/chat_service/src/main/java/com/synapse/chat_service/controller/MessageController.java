package com.synapse.chat_service.controller;

import java.util.UUID;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import com.synapse.chat_service.service.MessageService;
import com.synapse.chat_service_api.dto.request.MessageRequest;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @MessageMapping("/chat")
    public void handleChatMessage(MessageRequest.Chat chatMessage, @Header("simpUser") Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        messageService.processAndRespondToMessage(userId, chatMessage);
    }
}
