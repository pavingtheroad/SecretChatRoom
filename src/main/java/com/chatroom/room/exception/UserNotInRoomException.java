package com.chatroom.room.exception;

public class UserNotInRoomException extends RoomException {
    public UserNotInRoomException(String roomId) {
        super("USER_NOT_IN_ROOM", "User is NOT EXISTS in room" + roomId);
    }
}
