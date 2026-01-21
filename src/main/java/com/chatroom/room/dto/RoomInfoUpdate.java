package com.chatroom.room.dto;

public record RoomInfoUpdate(
        String roomName,
        String description,
        Boolean muted,
        Boolean locked,     // 房间是否公开
        Long ttlMillis      // 房间信息有效期
) {
}
