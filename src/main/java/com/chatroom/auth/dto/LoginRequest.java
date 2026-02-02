package com.chatroom.auth.dto;

public record LoginRequest(
        String userId,
        String password
) {
}
