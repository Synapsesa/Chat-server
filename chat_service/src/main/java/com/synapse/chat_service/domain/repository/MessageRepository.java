package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.Message;
import com.synapse.chat_service.dto.response.MessageResponse;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // 커서 기반 페이지네이션 - 최신순 (limit 직접 지정)
    @Query(value = "SELECT * FROM message WHERE user_id = :userId AND (:cursor IS NULL OR id < :cursor) ORDER BY id DESC LIMIT :limit", nativeQuery = true)
    List<MessageResponse.History> findByUserIdWithCursorDesc(@Param("userId") UUID userId, @Param("cursor") Long cursor, @Param("limit") int limit);
}
