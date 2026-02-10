package com.chatroom.websocket.dto;

import com.chatroom.websocket.enums.MessageType;

public record WsMessageRequest(
        MessageType type,
        String roomId,
        String content,
        String requestId
) {
}
