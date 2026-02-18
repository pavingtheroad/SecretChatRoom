package com.chatroom.user.dto;

public record UserInfoUpdate(
        String userId,
        String userName,
        String avatarUrl,
        String email
) {
}
