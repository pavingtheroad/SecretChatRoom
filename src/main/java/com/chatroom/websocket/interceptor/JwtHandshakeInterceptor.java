package com.chatroom.websocket.interceptor;

import com.chatroom.room.service.RoomService;
import com.chatroom.security.component.JwtProvider;
import com.chatroom.security.entity.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtProvider jwtProvider;
    private final RoomService roomService;
    public JwtHandshakeInterceptor(JwtProvider jwtProvider, RoomService roomService) {
        this.jwtProvider = jwtProvider;
        this.roomService = roomService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest servletRequest)){
            return false;
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);    // 去除Bearer

        Authentication authentication = jwtProvider.getAuthentication(token);
        SecurityUser securityUser = (SecurityUser)authentication.getPrincipal();    // 这里的principal只包含userPKId和roles
        attributes.put("SECURITY_USER", securityUser);

        String roomId = httpRequest.getParameter("roomId");
        if (!roomService.authorizeRoomAccess(roomId, securityUser.getUsername()))   // 用户是否属于要加入的房间，避免劫持roomId发起请求
            return false;
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
