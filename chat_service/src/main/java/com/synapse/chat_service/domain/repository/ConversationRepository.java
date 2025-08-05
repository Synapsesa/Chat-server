package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.Conversation;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    /**
     * 사용자 ID로 대화 조회 (각 사용자는 하나의 대화만 가짐)
     */
    Optional<Conversation> findByUserId(Long userId);
    
    /**
     * 사용자 ID로 대화 존재 여부 확인
     */
    boolean existsByUserId(Long userId);
}
