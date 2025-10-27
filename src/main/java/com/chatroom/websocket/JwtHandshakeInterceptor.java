package com.chatroom.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes){

        HttpHeaders headers = request.getHeaders();    // 从ws请求中拿到请求头
        var protocols = headers.get("Sec-WebSocket-Protocol");    // 通过subprotocol拿到token
        if (protocols != null && !protocols.isEmpty()) {
            var token = protocols.get(0).replace("Bearer_", "");    // 拿到Token
            System.out.println("握手时收到的Token: " + token);

            // 对token验证操作……

            attributes.put("token", token);
            response.getHeaders().add("Sec-WebSocket-Protocol", protocols.get(0));
        } else {
            System.out.println("握手时没有收到Token");
            response.setStatusCode(HttpStatusCode.valueOf(401));
            return false;
        }
        var queryParams = request.getURI().getQuery();
        if (queryParams != null && queryParams.contains("roomId=")){
            var roomId = getRoomIdFromQuery(queryParams);
            attributes.put("roomId", roomId);
        }
        else {
            log.error("{}握手请求缺少roomId参数", request.getURI());
            response.setStatusCode(HttpStatusCode.valueOf(400));
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception){

    }

    private String getRoomIdFromQuery(String query){
        String roomIdPrefix = "roomId=";
        int start = query.indexOf(roomIdPrefix);

        if (start != -1) {
            start += roomIdPrefix.length();
            int endIndex = query.indexOf('&', start);

            if (endIndex == -1) {
                return query.substring(start);    // 没有其他参数，取到字符串末尾
            } else {
                return query.substring(start, endIndex);
            }
        }
        throw new IllegalArgumentException("Invalid query without roomId");
    }
}
