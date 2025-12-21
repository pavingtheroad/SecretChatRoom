package com.chatroom.user.dto;

public record UserRegisterDTO(
        String userName,
        String password,
        String email,
        String avatarUrl
) {
}
