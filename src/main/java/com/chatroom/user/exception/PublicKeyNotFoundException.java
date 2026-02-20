package com.chatroom.user.exception;

public class PublicKeyNotFoundException extends UserException {
    public PublicKeyNotFoundException(String userId) {
        super("UNABLE_TO_GET_PUBLIC_KEY", "未找到" + userId + "用户的公钥");
    }
}
