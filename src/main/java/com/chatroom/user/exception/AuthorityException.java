package com.chatroom.user.exception;

public class AuthorityException extends UserException{
    public AuthorityException(String userId){
        super("AUTHORITY_ERROR", "User does not have the authority to operate:" + userId);
    }
}
