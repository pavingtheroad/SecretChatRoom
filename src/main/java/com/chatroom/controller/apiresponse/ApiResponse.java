package com.chatroom.controller.apiresponse;

public record ApiResponse<T>(
        String status,
        String message,
        T data,
        Object metaData
) {
}
