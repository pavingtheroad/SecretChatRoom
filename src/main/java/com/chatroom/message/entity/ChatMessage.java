package com.chatroom.message.entity;

import com.chatroom.message.domain.MessageType;

public record ChatMessage(
        String streamId,
        String roomId,
        String senderId,
        MessageType type,
        String content,
        Long createdAt
) {
}
