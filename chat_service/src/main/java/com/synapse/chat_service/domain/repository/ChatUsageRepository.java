package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.ChatUsage;

@Repository
public interface ChatUsageRepository extends JpaRepository<ChatUsage, Long> {
    
}
