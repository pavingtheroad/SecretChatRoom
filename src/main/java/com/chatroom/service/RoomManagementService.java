package com.chatroom.service;

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

    public void joinRoom(String roomId, WebSocketSession session){
        roomMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("{}加入房间{}", session.getAttributes().get("userId"), roomId);
    }

    public void leaveRoom(String roomId, WebSocketSession session){
        if (roomMap.computeIfPresent(roomId, (k, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        }).isEmpty()) {
            log.info("{}房间已清空", roomId);
        }
    }

    public void broadcast(String roomId, String msg) {
        Set<WebSocketSession> sessions = roomMap.get(roomId);
        if (sessions == null) return;
        for (WebSocketSession s : sessions) {
            sendSafely(s, msg);
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
