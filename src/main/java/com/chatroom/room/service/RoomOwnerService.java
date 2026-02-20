package com.chatroom.room.service;

import com.chatroom.room.dto.RoomInfoUpdate;

public interface RoomOwnerService {
    void addUserToRoom(String roomId, String userId);

    void manageRoomInfo(String roomId, RoomInfoUpdate roomInfo);

    void deleteRoom(String roomId);

    void putEncryptedKey(String roomId, String userPKId, String encryptedKey);
}