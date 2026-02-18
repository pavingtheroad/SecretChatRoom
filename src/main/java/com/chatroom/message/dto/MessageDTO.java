package com.chatroom.message.dto;

public record MessageDTO(
        String senderId,
        String type,
        String content,
        String createdAt
) {
}