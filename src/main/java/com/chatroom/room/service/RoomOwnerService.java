package com.chatroom.room.service;

import com.chatroom.room.dto.RoomInfoUpdate;

public interface RoomOwnerService {
    void addUserToRoom(String roomId, String userId, String operatorId);

    void manageRoomInfo(String roomId, RoomInfoUpdate roomInfo, String userId);

    void deleteRoom(String roomId, String userId);
}
