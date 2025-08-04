package com.synapse.chat_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.synapse.chat_service.domain.entity.ChatRoom;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    
    Page<ChatRoom> findByUserIdOrderByCreatedDateDesc(Long userId, Pageable pageable);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.userId = :userId AND cr.title LIKE %:keyword%")
    List<ChatRoom> findByUserIdAndTitleContaining(@Param("userId") Long userId, @Param("keyword") String keyword);
}
