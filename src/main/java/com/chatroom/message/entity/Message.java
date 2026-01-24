package com.chatroom.message.entity;

import com.chatroom.message.domain.MessageType;

public record Message(
        Long roomId,
        Long senderId,
        MessageType type,
        String content,
        Long createdAt
) {
}
