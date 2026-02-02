package com.chatroom.security.exception;

public class InvalidJwtException extends SecurityExceptions{
    public InvalidJwtException(String message){
        super("INVALID_JWT", message);
    }
}
