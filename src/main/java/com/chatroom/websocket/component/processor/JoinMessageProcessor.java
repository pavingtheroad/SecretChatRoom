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
public class JoinMessageProcessor implements MessageProcessor{
    private final RoomService roomService;
    private final MessageDispatcher messageDispatcher;
    private final SessionManager sessionManager;
    public JoinMessageProcessor(RoomService roomService,
                                MessageDispatcher messageDispatcher,
                                SessionManager sessionManager) {
        this.roomService = roomService;
        this.messageDispatcher = messageDispatcher;
        this.sessionManager = sessionManager;
    }
    @Override
    public MessageType supportType() {
        return MessageType.JOIN;
    }
    /**
     * JOIN消息处理：判断ctx中信息是否合法，用户是否有资格进入房间，若都合法则发送ACK给用户并使其加入房间session
     * @param ctx
     * @param request
     */
    @Override
    public void process(SessionContext ctx, WsMessageRequest request) {
        String userId = ctx.getUserId();
        if (ctx.getRoomId() != null){    // 已Join
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.alreadyJoinedRoom(request.requestId(), ctx));
            return;
        }
        String roomId = request.roomId();    // 请求加入的房间号
        if (userId == null || roomId == null){
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.invalidContext(request.requestId()));
            return;
        }
        if (!roomService.roomExists(roomId)){
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.roomNotFound(request.requestId(), roomId));
            return;
        }
        if (!roomService.authorizeRoomAccess(roomId, userId)){
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.notRoomMember(request.requestId()));
            return;
        }
        sessionManager.joinRoom(ctx.getSessionId(), roomId);
        messageDispatcher.sendAck(ctx.getSessionId(),
                WsMessageResponse.joinRoomAccepted(request.requestId(), roomId));
    }
}