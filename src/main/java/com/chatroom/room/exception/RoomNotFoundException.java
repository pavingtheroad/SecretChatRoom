package com.chatroom.room.exception;

public class RoomNotFoundException extends RoomException{
    public RoomNotFoundException(String roomId){
        super("ROOM_NOT_FOUND", "Room is not exists:" + roomId);
    }
}
