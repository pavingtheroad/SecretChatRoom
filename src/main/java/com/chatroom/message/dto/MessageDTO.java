package com.chatroom.message.dto;

import com.chatroom.message.domain.MessageType;

public record MessageDTO(
        String senderId,
        String type,
        String content,
        String createdAt
) {
}