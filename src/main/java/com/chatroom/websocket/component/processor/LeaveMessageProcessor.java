package com.chatroom.websocket.component.processor;

import com.chatroom.room.service.RoomService;
import com.chatroom.websocket.component.MessageDispatcher;
import com.chatroom.websocket.component.SessionManager;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.dto.WsMessageResponse;
import com.chatroom.websocket.enums.MessageType;
import org.springframework.stereotype.Component;

@Component
public class LeaveMessageProcessor implements MessageProcessor{
    private final SessionManager sessionManager;
    private final RoomService roomService;
    private final MessageDispatcher messageDispatcher;
    public LeaveMessageProcessor(SessionManager sessionManager, RoomService roomService, MessageDispatcher messageDispatcher) {
        this.sessionManager = sessionManager;
        this.roomService = roomService;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public MessageType supportType() {
        return MessageType.LEAVE;
    }

    /**
     * 确认用户是否在房间内 -> 离开房间( -> 发送离开房间的ACK报文
     * @param ctx
     * @param request
     */
    @Override
    public void process(SessionContext ctx, WsMessageRequest request) {
        if (request.roomId() == null){    // 非法请求
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.invalidContext(request.requestId()));
            return;
        }
        String roomId = ctx.getRoomId();
        if (roomId == null){    // 保持幂等性
            messageDispatcher.sendAck(ctx.getSessionId(),
                    WsMessageResponse.leaveRoomAccepted(request.requestId(), roomId));
            return;
        }
        sessionManager.leaveRoom(ctx.getSessionId());
        messageDispatcher.sendAck(ctx.getSessionId(),
                WsMessageResponse.leaveRoomAccepted(request.requestId(), roomId));
    }
}
