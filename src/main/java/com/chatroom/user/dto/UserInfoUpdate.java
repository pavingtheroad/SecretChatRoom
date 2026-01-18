package com.chatroom.user.dto;

public record UserInfoUpdate(
        String userName,
        String avatarUrl,
        String email,
        String userId
) {
}
