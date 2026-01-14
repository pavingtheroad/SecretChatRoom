package com.chatroom.user.entity;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoDTO;

public record UserEntity(
    Long id,
    String userId,
    String userName,
    String avatarUrl,
    String password,
    String email,
    UserStatus status
) {
    public UserInfoDTO toUserInfoDTO(){
        return new UserInfoDTO(
            this.userId(),
            this.userName(),
            this.avatarUrl(),
            this.status()
        );
    }
}
