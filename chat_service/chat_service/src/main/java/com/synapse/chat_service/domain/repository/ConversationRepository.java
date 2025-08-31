package com.synapse.chat_service.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.chat_service.domain.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByUserId(UUID userId);

    Optional<List<Conversation>> findByUserIdOrderByCreatedDateDesc(UUID userId);
}
