package com.chatroom.websocket.component.processor;

import com.chatroom.message.entity.ChatMessage;
import com.chatroom.message.service.MessageWriteService;
import com.chatroom.room.service.RoomService;
import com.chatroom.websocket.component.MessageDispatcher;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.dto.WsMessageResponse;
import com.chatroom.websocket.enums.MessageType;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TextMessageProcessor implements MessageProcessor{
    private final MessageWriteService messageWriteService;
    private final MessageDispatcher messageDispatcher;
    private final RoomService roomService;
    public TextMessageProcessor(MessageWriteService messageWriteService,
                                MessageDispatcher messageDispatcher,
                                RoomService roomService) {
        this.messageWriteService = messageWriteService;
        this.messageDispatcher = messageDispatcher;
        this.roomService = roomService;
    }
    @Override
    public MessageType supportType() {
        return MessageType.TEXT;
    }

    @Override
    public void process(SessionContext ctx, WsMessageRequest request) {
        String roomId = ctx.getRoomId();
        if (!Objects.equals(roomId, request.roomId())){
            return;
        }
        String userId = ctx.getUserId();
        if (roomId == null || userId == null){
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.invalidContext(request.requestId()));
            return;
        }
        if (!roomService.authorizeRoomAccess(roomId, userId)){
            messageDispatcher.sendError(ctx.getSessionId(),
                    WsMessageResponse.notRoomMember(request.requestId()));
            return;
        }
        ChatMessage msg = messageWriteService.saveMessage(userId, roomId, request.content(), request.requestId());
        if (msg == null){
            messageDispatcher.sendAck(ctx.getSessionId(),
                    WsMessageResponse.messageAccepted(request.requestId()));
            return;
        }
        messageDispatcher.sendAck(ctx.getSessionId(),
                WsMessageResponse.messageAccepted(request.requestId()));
        messageDispatcher.broadcastTextToRoom(roomId,msg);
    }
}
