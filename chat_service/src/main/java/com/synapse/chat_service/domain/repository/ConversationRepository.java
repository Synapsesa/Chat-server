package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.Conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByUserId(UUID userId);
    Optional<List<Conversation>> findByUserIdOrderByCreatedAtDesc(UUID userId);

}
