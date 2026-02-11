package com.chatroom.websocket.enums;

public enum WsErrorCode {
    /**
     * 无效的会话
     */
    INVALID_CONTEXT,
    /**
     * 用户不存在
     */
    NOT_ROOM_MEMBER,
    /**
     * 房间不存在
     */
    ROOM_NOT_FOUND,
    /**
     * 权限不足
     */
    PERMISSION_DENIED,
    /**
     * 已加入房间
     */
    ALREADY_JOINED,
    /**
     * 消息格式错误
     */
    INVALID_MESSAGE_FORMAT,
    /**
     * 暂不支持的消息类型
     */
    INVALID_MESSAGE_TYPE
}
