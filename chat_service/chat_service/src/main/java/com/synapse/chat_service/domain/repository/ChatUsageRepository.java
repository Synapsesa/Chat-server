package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.synapse.chat_service.domain.entity.ChatUsage;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, Long> {

}
