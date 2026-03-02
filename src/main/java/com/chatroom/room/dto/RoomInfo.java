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
    public static RoomInfo empty(){
        return new RoomInfo("", "", "", "", 0L, false, false, 30000L);
    }
}
