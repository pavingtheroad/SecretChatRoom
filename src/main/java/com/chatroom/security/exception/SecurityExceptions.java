package com.chatroom.security.exception;

public class SecurityExceptions extends RuntimeException{
    private final String errorCode;
    public SecurityExceptions(String code, String msg){
        super(msg);
        this.errorCode = code;
    }
    public String getErrorCode(){
        return errorCode;
    }
}
