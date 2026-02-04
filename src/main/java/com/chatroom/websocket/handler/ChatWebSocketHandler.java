package com.chatroom.websocket.handler;

import com.chatroom.message.domain.MessageType;
import com.chatroom.message.service.MessageWriteService;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final MessageWriteService messageWriteService;
    private final ObjectMapper objectMapper;
    public ChatWebSocketHandler(MessageWriteService messageWriteService, ObjectMapper objectMapper) {
        this.messageWriteService = messageWriteService;
        this.objectMapper = objectMapper;
    }
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message){
        try {
            SecurityUser user = getUser(session);
            String payload = message.getPayload();
            WsMessageRequest request = objectMapper.readValue(payload, WsMessageRequest.class);
            if (request.roomId() == null){
                throw new RuntimeException("roomId is null");
            }
            if (request.type().equals(MessageType.TEXT)){
                messageWriteService.sendMessage(Long.parseLong(request.roomId()), request.content());
            }

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    private SecurityUser getUser(WebSocketSession session) {
        return (SecurityUser) session.getAttributes().get("SECURITY_USER");
    }
}
