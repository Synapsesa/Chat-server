package com.synapse.chat_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.Message;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findByChatRoomIdOrderByCreatedDateAsc(UUID chatRoomId);
    
    Page<Message> findByChatRoomIdOrderByCreatedDateAsc(UUID chatRoomId, Pageable pageable);
    
    Page<Message> findByChatRoomIdOrderByCreatedDateDesc(UUID chatRoomId, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.chatRoom.id = :chatRoomId AND m.content LIKE %:keyword%")
    List<Message> findByChatRoomIdAndContentContaining(@Param("chatRoomId") UUID chatRoomId, @Param("keyword") String keyword);
    
    long countByChatRoomId(UUID chatRoomId);
}
