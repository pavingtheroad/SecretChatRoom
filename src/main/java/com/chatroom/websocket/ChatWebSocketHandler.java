// 核心通信逻辑
package com.chatroom.websocket;

import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.service.MessageService;
import com.chatroom.room.service.RoomManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final RoomManagementService roomManagementService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");     // 从session中获取roomId
        roomManagementService.joinRoom(roomId, session);
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage  message){
        String payload = message.getPayload();
        MessageDTO msg = objectMapper.convertValue(payload, MessageDTO.class);
        msg.setUserId((String) session.getAttributes().get("userPKId"));
        msg.setRoomId((String) session.getAttributes().get("roomId"));
        messageService.saveAndBroadcast(msg, msg.getRoomId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = (String) session.getAttributes().get("roomId");
        roomManagementService.leaveRoom(roomId, session);
    }
}
