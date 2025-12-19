package com.chatroom.room.exception;

public class RoomException extends Exception{
    private final String errorCode;
    public RoomException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
}
