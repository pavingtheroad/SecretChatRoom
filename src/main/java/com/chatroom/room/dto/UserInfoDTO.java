package com.chatroom.room.dto;

public record UserInfoDTO(
        String userId,
        String userName,
        String avatarUrl    // 头像URL
) {
}
