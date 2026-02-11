package com.chatroom.websocket.component.processor;

import com.chatroom.websocket.component.MessageDispatcher;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.dto.WsMessageResponse;
import com.chatroom.websocket.enums.MessageType;

public class HeartbeatMessageProcessor implements MessageProcessor{
    private final MessageDispatcher messageDispatcher;
    public HeartbeatMessageProcessor(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }
    @Override
    public MessageType supportType() {
        return MessageType.HEARTBEAT;
    }

    @Override
    public void process(SessionContext ctx, WsMessageRequest request) {
        ctx.updateActiveTime();
        messageDispatcher.sendAck(ctx.getSessionId(),
                WsMessageResponse.heartbeatAck(request.requestId())
        );
    }
}
