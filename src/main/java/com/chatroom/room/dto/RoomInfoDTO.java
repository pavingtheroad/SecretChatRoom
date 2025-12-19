package com.chatroom.room.dto;

public record RoomInfoDTO(
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
