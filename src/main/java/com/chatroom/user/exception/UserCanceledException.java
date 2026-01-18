package com.chatroom.user.exception;

public class UserCanceledException extends UserException {
    public UserCanceledException(String userId) {
        super("USER_HAS_BEEN_CANCELED", userId + " has been canceled");
    }
}
