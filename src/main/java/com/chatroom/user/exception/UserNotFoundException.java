package com.chatroom.user.exception;

import com.chatroom.room.exception.RoomException;

public class UserNotFoundException extends RoomException {
    public UserNotFoundException(String message){
        super("USER_NOT_FOUND", message);
    }
}
