package com.chatroom.user.exception;

public class UserException extends Exception{
    private final String errorCode;
    public UserException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public String getErrorCode() {
        return errorCode;
    }
}
