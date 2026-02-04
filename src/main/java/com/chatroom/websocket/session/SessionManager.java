package com.chatroom.websocket.session;

import com.chatroom.websocket.domain.SessionContext;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    // <sessionId, SessionContext>
    private final ConcurrentHashMap<String, SessionContext> sessionContextMap = new ConcurrentHashMap<>();
    // <roomId, <sessionId>> ConcurrentHashMap.newKeySet()
    private final ConcurrentHashMap<String, Set<String>> roomContext = new ConcurrentHashMap<>();
    // <userId, <sessionId>>
    private final ConcurrentHashMap<String, Set<String>> userContext = new ConcurrentHashMap<>();
    /**
     * 添加连接会话
     * @param session
     * 创建新SessionContext并保存进session，socketSession直接作为WebsocketSession成员、sessionId为websocketSession.getId()
     */
    public void register(WebSocketSession session){
        SessionContext sessionContext = new SessionContext(session.getId(), session);
        sessionContextMap.put(session.getId(), sessionContext);
    }
    /**
     * 绑定UserId;
     * 若检测到sessionId的会话中已绑定了UserId，则返回false/异常
     * 若userContext的userId键没有值，则创建ConcurrentHashMap.newKeySet()并加入sessionId; 否则添加sessionId
     */
    public void bindUserId(String sessionId, Long userId){
        SessionContext sessionContext = sessionContextMap.get(sessionId);
        if (sessionContext == null){
            throw new IllegalArgumentException("sessionId not found");
        }

        String uid = String.valueOf(userId);
        String oldUserId = sessionContext.getUserId() == null ? null : String.valueOf(sessionContext.getUserId());
        if (Objects.equals(oldUserId, uid)) {
            return; // 幂等
        }
        // 解绑旧的userId
        if (oldUserId != null) {
            Set<String> sessions = userContext.get(oldUserId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {    // 如果该userId没有会话，则删除userId
                    userContext.remove(oldUserId);
                }
            }
        }
        userContext.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionContext.setUserId(uid);
    }
    /**
     * 绑定Room;
     * 若检测到sessionId的会话中已绑定了RoomId，则返回false/异常
     */
    public void joinRoom(String sessionId, String roomId){
        SessionContext sessionContext = sessionContextMap.get(sessionId);
        if (sessionContext == null){
            throw new IllegalArgumentException("sessionId not found");
        }
        String oldRoomId = sessionContext.getRoomId();
        if (Objects.equals(oldRoomId, roomId)){
            return;
        }
        if (oldRoomId != null){
            Set<String> sessions = roomContext.get(oldRoomId);
            if (sessions != null){
                sessions.remove(sessionId);
                if (sessions.isEmpty()){
                    roomContext.remove(oldRoomId);
                }
            }
        }
        roomContext.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionContext.setRoomId(roomId);
    }
    /**
     * 离开房间
     */
    public void leaveRoom(String sessionId){
        SessionContext sessionContext = sessionContextMap.get(sessionId);
        if (sessionContext == null){
            throw new IllegalArgumentException("sessionId not found");
        }

        String roomId = sessionContext.getRoomId();
        if (roomId == null){
            return; // 幂等
        }

        Set<String> sessions = roomContext.get(roomId);
        if (sessions != null){
            sessions.remove(sessionId);
            if (sessions.isEmpty()){
                roomContext.remove(roomId);
            }
        }

        sessionContext.setRoomId(null);
    }
    /**
     * 关闭会话;
     * 从sessionContextMap中删除sessionId的会话，并删除roomContext、userContext的sessionId
     * 若roomContext、userContext的Set为空，则删除键
     * 关闭websocketSession
     */
    public void closeSession(String sessionId) throws IOException {
        SessionContext sessionContext = sessionContextMap.remove(sessionId);    // 删除sessionId的会话
        if (sessionContext != null){
            String roomId = sessionContext.getRoomId();    // 清理房间会话索引
            if (roomId != null){
                Set<String> sessions = roomContext.get(roomId);
                if (sessions != null){
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()){
                        roomContext.remove(roomId);
                    }
                }
            }
            String userId = sessionContext.getUserId();    // 清理用户会话索引
            if (userId != null){
                Set<String> sessions = userContext.get(userId);
                if (sessions != null){
                    sessions.remove(sessionId);
                    if (sessions.isEmpty()){
                        userContext.remove(userId);
                    }
                }
            }
        }
        sessionContext.getSession().close();
    }
    /**
     * 获取SessionContext
     */
    public SessionContext getSessionContext(String sessionId){
        return sessionContextMap.get(sessionId);
    }
    /**
     * 获取房间内的所有会话Id
     */
    public Set<String> getRoomSessionsId(String roomId){
        return roomContext.get(roomId);
    }
    /**
     * 获取用户所有的会话Id
     */
    public Set<String> getUserSessionsId(String userId){
        return userContext.get(userId);
    }
}
