package com.chatroom.websocket.component.processor;

import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.enums.MessageType;

public interface MessageProcessor {
    MessageType supportType();
    void process(SessionContext ctx, WsMessageRequest request);
}
