package com.chatroom.room.dto;

public record RoomInfo(
        String roomId,
        String roomName,
        String ownerId,
        String description,
        Long createdAt,
        Boolean muted,
        Boolean locked,
        Long ttlMillis
) {

}
