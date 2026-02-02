package com.chatroom.security.exception;

public class JwtGenerateException extends SecurityExceptions{
    public JwtGenerateException(String msg){
        super("JWT_GENERATE_ERROR", "Jwt generate error for:" + msg);
    }
}
