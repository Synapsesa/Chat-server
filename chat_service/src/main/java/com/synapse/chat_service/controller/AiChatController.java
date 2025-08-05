package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {
    
    private final MessageService messageService;
    
    @GetMapping("/history")
    public ResponseEntity<List<MessageResponse.Simple>> getMyAiChatHistory(
        @RequestHeader("X-User-Id") Long userId
    ) {
        List<MessageResponse.Simple> response = messageService.getMessagesByUserId(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/history/paging")
    public ResponseEntity<Page<MessageResponse.Simple>> getMyAiChatHistoryWithPaging(
        @RequestHeader("X-User-Id") Long userId,
        @PageableDefault(size = 50, sort = "createdDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<MessageResponse.Simple> response = messageService.getMessagesByUserIdWithPaging(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/history/recent")
    public ResponseEntity<Page<MessageResponse.Simple>> getMyAiChatHistoryRecentFirst(
        @RequestHeader("X-User-Id") Long userId,
        @PageableDefault(size = 50, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageResponse.Simple> response = messageService.getMessagesRecentFirst(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<MessageResponse.Simple>> searchMyAiChatHistory(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam String keyword
    ) {
        List<MessageResponse.Simple> response = messageService.searchMessages(userId, keyword);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<AiChatStatsResponse> getMyAiChatStats(
        @RequestHeader("X-User-Id") Long userId
    ) {
        long messageCount = messageService.getMessageCountByUserId(userId);
        UUID conversationId = messageService.getConversationId(userId);
        
        AiChatStatsResponse response = new AiChatStatsResponse(
            conversationId,
            messageCount
        );
        
        return ResponseEntity.ok(response);
    }
    
    public record AiChatStatsResponse(
        UUID conversationId,
        long totalMessageCount
    ) {}
}
