package com.synapse.chat_service.domain.entity;

import com.synapse.chat_service.domain.common.BaseTimeEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {
    
    @Id
    @Column(name = "id", columnDefinition = "BIGINT")
    private Long id;
    
    @NotBlank
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @NotBlank
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @OneToMany(mappedBy = "userId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoom> chatRooms = new ArrayList<>();
    
    @Builder
    public User(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
}
