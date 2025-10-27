// 核心通信逻辑
package com.chatroom.websocket;

import com.chatroom.service.RoomManagementService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final RoomManagementService roomManagementService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        roomManagementService.joinRoom(roomId, session);
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage  message){
        String payload = message.getPayload();
        String roomId = (String) session.getAttributes().get("roomId");
        roomManagementService.broadcast(roomId, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        roomManagementService.leaveRoom(roomId, session);
    }
}
