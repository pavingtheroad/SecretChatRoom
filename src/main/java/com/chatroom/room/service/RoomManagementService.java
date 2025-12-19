package com.chatroom.room.service;

import com.chatroom.message.dto.MessageDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RoomManagementService {
    private final ConcurrentHashMap<String, Set<WebSocketSession>> roomMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void joinRoom(String roomId, WebSocketSession session){
        roomMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("{}加入房间{}", session.getAttributes().get("userId"), roomId);
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

    public void broadcast(String roomId, MessageDTO msg){
        Set<WebSocketSession> sessions = roomMap.get(roomId);
        if (sessions == null) return;
        for (WebSocketSession s : sessions) {
            try{
                sendSafely(s, objectMapper.writeValueAsString(msg));
            } catch (JsonProcessingException e){
                log.error("JsonProcessingIssue while broadcasting,{}",e);
            }
        }
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
