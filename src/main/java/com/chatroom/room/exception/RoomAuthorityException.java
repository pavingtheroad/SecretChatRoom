package com.chatroom.room.exception;

public class RoomAuthorityException extends RoomException {
    public RoomAuthorityException(String roomId) {
        super("WRONG_AUTHORITY_TO_OPERATE_ROOM", "You do not have permission to operate room" + roomId);
    }
}
