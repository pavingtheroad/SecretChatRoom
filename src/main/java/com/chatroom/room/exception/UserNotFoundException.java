package com.chatroom.room.exception;

public class UserNotFoundException extends RoomException{
    public UserNotFoundException(String message){
        super("USER_NOT_FOUND", message);
    }
}
