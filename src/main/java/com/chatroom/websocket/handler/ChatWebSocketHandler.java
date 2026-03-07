package com.chatroom.websocket.handler;

import com.chatroom.message.service.MessageWriteService;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.websocket.component.MessageDispatcher;
import com.chatroom.websocket.component.SessionManager;
import com.chatroom.websocket.component.processor.MessageProcessor;
import com.chatroom.websocket.component.processor.MessageProcessorRouter;
import com.chatroom.websocket.domain.SessionContext;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.dto.WsMessageResponse;
import com.chatroom.websocket.enums.MessageType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final SessionManager sessionManager;
    private final MessageProcessorRouter router;
    private final MessageDispatcher messageDispatcher;
    public ChatWebSocketHandler(ObjectMapper objectMapper,
                                SessionManager sessionManager,
                                MessageProcessorRouter router,
                                MessageDispatcher messageDispatcher) {
        this.objectMapper = objectMapper;
        this.sessionManager = sessionManager;
        this.router = router;
        this.messageDispatcher = messageDispatcher;
    }

    /**
     * 注册session，绑定进用户
     * @param session
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        System.out.println("REGISTER " + session.getId());
        sessionManager.register(session);
        SecurityUser user = getUser(session);
        if (user != null){
            sessionManager.bindUserId(session.getId(), user.getUsername());
        }
    }

    /**
     * request接受解析后TextMessage -> 根据请求类型进行条件判断决定响应
     * @param session
     * @param message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("HANDLE " + session.getId());
        System.out.println("CTX " + sessionManager.getSessionContext(session.getId()));
        WsMessageRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), WsMessageRequest.class);
        } catch (JsonProcessingException e) {
            // 非法 JSON，直接协议级错误
            messageDispatcher.sendError(
                    session.getId(),
                    WsMessageResponse.invalidMessageFormat(null)
            );
            return;
        }
        MessageType type = request.type();
        if (type == null){
            messageDispatcher.sendError(session.getId(),
                    WsMessageResponse.invalidMessageType(request.requestId())
            );
            return;
        }
        MessageProcessor processor = router.getProcessor(type);
        if (processor == null){
            return;
        }
        SessionContext ctx = sessionManager.getSessionContext(session.getId());
        if (ctx == null) {
            // 理论上极少
            return;
        }
        ctx.updateActiveTime();
        processor.process(ctx, request);
    }
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception){
        sessionManager.closeSession(session.getId());
    }
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        System.out.println("CLOSE SESSION " + session.getId());
        sessionManager.closeSession(session.getId());
    }
    /**
     * 从session中获取用户信息，其配置来源于JwtHandshakeInterceptor的beforeHandshake方法, 只包含userPKId和roles
     * @param session
     * @return
     */
    private SecurityUser getUser(WebSocketSession session) {
        return (SecurityUser) session.getAttributes().get("SECURITY_USER");
    }
    /**
     * 从session中获取建立连接的房间号
     * @param session
     * @return
     */
    private String getRoomId(WebSocketSession session) {
        return session.getAttributes().get("ROOM_ID").toString();
    }
}
