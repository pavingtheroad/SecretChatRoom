package com.chatroom.websocket.component;

import com.chatroom.message.entity.ChatMessage;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.util.Set;

@Component
public class MessageDispatcher {
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    public MessageDispatcher(SessionManager sessionManager, ObjectMapper objectMapper) {
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }
    public void deliverToSession(String sessionId, TextMessage msg) {
        SessionContext ctx = sessionManager.getSessionContext(sessionId);
        if (ctx == null) return;

        try {
            synchronized (ctx.getSendLock()){
                ctx.getSession().sendMessage(msg);
            }
        } catch (IOException e) {
            // 网络失败是常态，不上抛
            sessionManager.closeSession(sessionId);
        }
    }
    /**
     * 广播文字消息到房间
     * @param roomId
     * @param msg
     */
    private void broadcastToRoom(String roomId, TextMessage msg){
        Set<String> sessionsId = sessionManager.getRoomSessionsId(roomId);
        if (sessionsId == null) return;
        for (String sessionId : sessionsId){
            deliverToSession(sessionId, msg);
        }
    }
    public void broadcastTextToRoom(String roomId, ChatMessage message){
        WsMessageResponse response = WsMessageResponse.text(message);
        broadcastToRoom(roomId, toTextMessage(response));
    }
    /**
     * 用户多端控制，文字消息同步到用户多端连接，检索用户
     * @param userId
     * @param msg
     */
    public void syncToUser(String userId, TextMessage msg) {
        Set<String> sessionsId = sessionManager.getUserSessionsId(userId);
        for (String sessionId : sessionsId) {
            deliverToSession(sessionId, msg);
        }
    }
    // 协议层级ERROR
    public void sendError(String sessionId, WsMessageResponse error){
        deliverToSession(sessionId, toTextMessage(error));
    }
    public void sendAck(String sessionId, WsMessageResponse ack){
        deliverToSession(sessionId, toTextMessage(ack));
    }
    // WsMessageResponse -> JSON -> TextMessage
    public TextMessage toTextMessage(WsMessageResponse response){
        try {
            String json = objectMapper.writeValueAsString(response);
            return new TextMessage(json);
        } catch (JsonProcessingException e){
            throw new IllegalStateException("Serialize WsMessageResponse failed", e);
//          其他处理逻辑？
        }
    }
}
