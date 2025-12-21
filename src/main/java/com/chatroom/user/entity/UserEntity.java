package com.chatroom.user.entity;

import com.chatroom.user.domain.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    private Long id;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String password;
    private String email;
    private UserStatus status;
}
