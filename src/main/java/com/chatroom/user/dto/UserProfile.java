package com.chatroom.user.dto;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.entity.UserEntity;

public record UserProfile(
        String userId,
        String userName,
        String avatarUrl,
        String email,
        UserStatus status
) {
    public static UserProfile fromEntity(UserEntity userEntity){
        return new UserProfile(
                userEntity.userId(),
                userEntity.userName(),
                userEntity.avatarUrl(),
                userEntity.email(),
                userEntity.status()
        );
    }
}
