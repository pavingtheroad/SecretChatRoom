package com.chatroom.user.dto;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.entity.UserEntity;


public record UserInfoDTO(
        String userId,
        String userName,
        String avatarUrl,
        UserStatus status
) {
}
