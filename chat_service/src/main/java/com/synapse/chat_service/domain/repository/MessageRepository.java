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
    
    List<Message> findByConversationIdOrderByCreatedDateAsc(UUID conversationId);
    
    Page<Message> findByConversationIdOrderByCreatedDateAsc(UUID conversationId, Pageable pageable);
    
    Page<Message> findByConversationIdOrderByCreatedDateDesc(UUID conversationId, Pageable pageable);
    
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.content LIKE %:keyword%")
    List<Message> findByConversationIdAndContentContaining(@Param("conversationId") UUID conversationId, @Param("keyword") String keyword);
    
    long countByConversationId(UUID conversationId);
}
