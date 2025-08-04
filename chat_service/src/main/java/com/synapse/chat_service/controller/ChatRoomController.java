package com.synapse.chat_service.controller;

import com.synapse.chat_service.dto.request.ChatRoomRequest;
import com.synapse.chat_service.dto.response.ChatRoomResponse;
import com.synapse.chat_service.service.ChatRoomService;
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
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {
    
    private final ChatRoomService chatRoomService;
    
    /**
     * 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ChatRoomResponse.Detail> createChatRoom(
        @Valid @RequestBody ChatRoomRequest.Create request
    ) {
        ChatRoomResponse.Detail response = chatRoomService.createChatRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 채팅방 단건 조회
     */
    @GetMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomResponse.Detail> getChatRoom(
        @PathVariable UUID chatRoomId
    ) {
        ChatRoomResponse.Detail response = chatRoomService.getChatRoom(chatRoomId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자별 채팅방 목록 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<ChatRoomResponse.Simple>> getChatRoomsByUserId(
        @RequestParam Long userId,
        @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChatRoomResponse.Simple> response = chatRoomService.getChatRoomsByUserId(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방 제목 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<ChatRoomResponse.Simple>> searchChatRooms(
        @RequestParam Long userId,
        @RequestParam String keyword
    ) {
        List<ChatRoomResponse.Simple> response = chatRoomService.searchChatRooms(userId, keyword);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방 수정
     */
    @PutMapping("/{chatRoomId}")
    public ResponseEntity<ChatRoomResponse.Detail> updateChatRoom(
        @PathVariable UUID chatRoomId,
        @Valid @RequestBody ChatRoomRequest.Update request
    ) {
        ChatRoomResponse.Detail response = chatRoomService.updateChatRoom(chatRoomId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 채팅방 삭제
     */
    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable UUID chatRoomId) {
        chatRoomService.deleteChatRoom(chatRoomId);
        return ResponseEntity.noContent().build();
    }
}
