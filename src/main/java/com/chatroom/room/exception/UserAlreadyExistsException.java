package com.chatroom.room.exception;

public class UserAlreadyExistsException extends RoomException{
    public UserAlreadyExistsException(String userId){
        super("USER_ALREADY_EXISTS", "User already exists: " + userId);
    }
}
