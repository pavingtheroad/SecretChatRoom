package com.chatroom.room.service;

import com.chatroom.message.dto.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomManagementService {
    private final ConcurrentHashMap<String, Set<WebSocketSession>> roomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static Logger log = LoggerFactory.getLogger(RoomManagementService.class);

    public void joinRoom(String roomId, WebSocketSession session){
        roomMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("{}加入房间{}", session.getAttributes().get("userPKId"), roomId);
    }

    public void leaveRoom(String roomId, WebSocketSession session){
        roomMap.compute(roomId, (k, sessions) -> {
            if (sessions == null) return null;
            sessions.remove(session);
            if (sessions.isEmpty()) {
                log.info("房间 {} 已清空", roomId);
                return null;
            }
            return sessions;
        });
    }

    private void sendSafely(WebSocketSession session, String msg) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(msg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
