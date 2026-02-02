package com.chatroom.util;

public record ApiResponse<T>(
        String status,
        String message,
        T data,
        Object metaData
) {
}
