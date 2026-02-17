package com.chatroom.websocket.domain;

import org.springframework.web.socket.WebSocketSession;

public class SessionContext {
    private final String sessionId;
    private final WebSocketSession session;
    private volatile String userId;    // token认证后绑定
    private volatile String roomId;    // join 后绑定
    private volatile long activeTime;    // 最新活跃时间
    private final Object sendLock = new Object();    // 发送锁
    public SessionContext(String sessionId, WebSocketSession session) {
        this.sessionId = sessionId;
        this.session = session;
    }

    public String getSessionId() {
        return sessionId;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getActiveTime() {
        return activeTime;
    }
    public void updateActiveTime() {
        this.activeTime = System.currentTimeMillis();
    }

    public Object getSendLock() {
        return sendLock;
    }
}
