package com.chatroom.room.dto;

public record PutEncryptedKeyRequest(
        String userId,
        String encryptedKey
) {
}
