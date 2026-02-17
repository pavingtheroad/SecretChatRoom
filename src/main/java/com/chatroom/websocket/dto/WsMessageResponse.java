package com.chatroom.websocket.dto;

import com.chatroom.message.entity.ChatMessage;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.enums.WsAckCode;
import com.chatroom.websocket.enums.WsErrorCode;
import com.chatroom.websocket.enums.WsResponseType;

public record WsMessageResponse(
        WsResponseType type,     // ACK | ERROR | TEXT
        String requestId,        // ACK / ERROR 对应请求；TEXT 可为 null
        String roomId,
        String sender,
        String code,             // ERROR 使用
        String content,
        Long serverTimestamp
) {
    public static WsMessageResponse text(ChatMessage message){
        return new WsMessageResponse(
                WsResponseType.TEXT,
                null,
                message.roomId(),
                message.senderId(),
                null,
                message.content(),
                message.createdAt()
        );
    }
    public static WsMessageResponse invalidContext(String requestId) {
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                null,
                null,
                WsErrorCode.INVALID_CONTEXT.name(),
                "REJECT",
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse invalidMessageType(String requestId){
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                null,
                null,
                WsErrorCode.INVALID_MESSAGE_TYPE.name(),
                "REJECT",
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse notRoomMember(String requestId) {
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                null,
                null,
                WsErrorCode.NOT_ROOM_MEMBER.name(),
                "REJECT",
                System.currentTimeMillis()
        );
    }

    public static WsMessageResponse roomNotFound(String requestId, String roomId) {
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                roomId,
                null,
                WsErrorCode.ROOM_NOT_FOUND.name(),
                "Room not found",
                System.currentTimeMillis()
        );
    }
    // 已加入过房间的报错报文 Already joined a room
    public static WsMessageResponse alreadyJoinedRoom(String requestId, SessionContext ctx) {
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                ctx.getRoomId(),
                ctx.getUserId(),
                WsErrorCode.ALREADY_JOINED.name(),
                "Already joined a room",
                System.currentTimeMillis()
        );
    }
    // 确认加入房间的ACK报文
    public static WsMessageResponse joinRoomAccepted(
            String requestId,
            String roomId
    ) {
        return new WsMessageResponse(
                WsResponseType.ACK,
                requestId,
                roomId,
                null,
                null,
                "Join into room",
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse leaveRoomAccepted(
            String requestId,
            String roomId
    ) {
        return new WsMessageResponse(
                WsResponseType.ACK,
                requestId,
                roomId,
                null,
                WsAckCode.LEAVE_ROOM_SUCCESS.name(),
                "OK",
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse invalidMessageFormat(String requestId){
        return new WsMessageResponse(
                WsResponseType.ERROR,
                requestId,
                null,
                null,
                WsErrorCode.INVALID_MESSAGE_FORMAT.name(),
                "REJECT",
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse messageAccepted(
            String requestId
    ) {
        return new WsMessageResponse(
                WsResponseType.ACK,
                requestId,
                null,
                null,
                null,
                "OK", // 未来更新为生成的messageId
                System.currentTimeMillis()
        );
    }
    public static WsMessageResponse heartbeatAck(String requestId) {
        return new WsMessageResponse(
                WsResponseType.ACK,
                requestId,
                null,
                null,
                null,
                "OK",
                System.currentTimeMillis()
        );
    }

}
