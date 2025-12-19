package com.chatroom.room.exception;

public class RoomAlreadyExistsException extends RoomException{
    public RoomAlreadyExistsException(String roomId) {
        super("ROOM_ALREADY_EXISTS", "Room already exists：" + roomId);
    }
}
