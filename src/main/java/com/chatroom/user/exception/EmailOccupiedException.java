package com.chatroom.user.exception;

public class EmailOccupiedException extends UserException{
    public EmailOccupiedException(String emailAddress) {
        super("EMAIL_EXISTS", "Email has been registered:" + emailAddress);
    }
}
