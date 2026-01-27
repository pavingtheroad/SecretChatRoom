package com.chatroom.message.dto;


public record MessageDTO(
        String roomId,
        String senderId,
        String type,
        String content
) {
}