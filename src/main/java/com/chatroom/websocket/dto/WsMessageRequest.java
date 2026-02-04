package com.chatroom.websocket.dto;

import com.chatroom.message.domain.MessageType;

public record WsMessageRequest(
        String roomId,
        MessageType type,
        String content
) {
}
