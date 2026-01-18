package com.chatroom.user.exception;

public class UserIdLostConnection extends UserException {
    public UserIdLostConnection(String userId) {
        super("USER_ID_LOST_CONNECTION", userId + "失去与数据库的关联");
    }
}
