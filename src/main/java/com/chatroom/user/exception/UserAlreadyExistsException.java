package com.chatroom.user.exception;

import com.chatroom.room.exception.RoomException;

public class UserAlreadyExistsException extends RoomException {
    public UserAlreadyExistsException(String userId){
        super("USER_ALREADY_EXISTS", "User already exists: " + userId);
    }
}
