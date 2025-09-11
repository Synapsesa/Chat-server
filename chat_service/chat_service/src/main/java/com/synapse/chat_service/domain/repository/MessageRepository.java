package com.synapse.chat_service.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service_api.dto.response.MessageResponse;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query(value = "SELECT m.* FROM messages m JOIN conversations c ON m.conversation_id = c.conversation_id WHERE c.user_id = :userId AND (:cursor IS NULL OR m.id < :cursor) ORDER BY m.id DESC LIMIT :limit", nativeQuery = true)
    List<MessageResponse.History> findByUserIdWithCursorDesc(
        @Param("userId") UUID userId, 
        @Param("cursor") Long cursor,
        @Param("limit") int limit
    );
}
