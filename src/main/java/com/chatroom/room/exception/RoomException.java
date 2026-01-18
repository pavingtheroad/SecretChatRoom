package com.chatroom.room.exception;

public class RoomException extends RuntimeException{
    private final String errorCode;
    public RoomException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
}
