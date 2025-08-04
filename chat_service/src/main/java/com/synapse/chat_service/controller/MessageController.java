package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.request.MessageRequest;
import com.synapse.chat_service.dto.response.MessageResponse;
import com.synapse.chat_service.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {
    
    private final MessageService messageService;
    
    /**
     * 메시지 생성
     */
    @PostMapping
    public ResponseEntity<MessageResponse.Detail> createMessage(
        @Valid @RequestBody MessageRequest.Create request
    ) {
        MessageResponse.Detail response = messageService.createMessage(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 메시지 단건 조회
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<MessageResponse.Detail> getMessage(
        @PathVariable Long messageId
    ) {
        MessageResponse.Detail response = messageService.getMessage(messageId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방별 메시지 목록 조회 (시간순 정렬)
     */
    @GetMapping("/chat-room/{chatRoomId}")
    public ResponseEntity<List<MessageResponse.Simple>> getMessagesByChatRoomId(
        @PathVariable UUID chatRoomId
    ) {
        List<MessageResponse.Simple> response = messageService.getMessagesByChatRoomId(chatRoomId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방별 메시지 목록 조회 (페이징, 시간순 정렬)
     */
    @GetMapping("/chat-room/{chatRoomId}/paging")
    public ResponseEntity<Page<MessageResponse.Simple>> getMessagesByChatRoomIdWithPaging(
        @PathVariable UUID chatRoomId,
        @PageableDefault(size = 50, sort = "createdDate", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<MessageResponse.Simple> response = messageService.getMessagesByChatRoomIdWithPaging(chatRoomId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방별 메시지 목록 조회 (페이징, 최신순 정렬)
     */
    @GetMapping("/chat-room/{chatRoomId}/recent")
    public ResponseEntity<Page<MessageResponse.Simple>> getMessagesRecentFirst(
        @PathVariable UUID chatRoomId,
        @PageableDefault(size = 50, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageResponse.Simple> response = messageService.getMessagesRecentFirst(chatRoomId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 메시지 내용 검색
     */
    @GetMapping("/chat-room/{chatRoomId}/search")
    public ResponseEntity<List<MessageResponse.Simple>> searchMessages(
        @PathVariable UUID chatRoomId,
        @RequestParam String keyword
    ) {
        List<MessageResponse.Simple> response = messageService.searchMessages(chatRoomId, keyword);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 메시지 삭제
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return ResponseEntity.noContent().build();
    }
}
