package com.synapse.chat_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

}
