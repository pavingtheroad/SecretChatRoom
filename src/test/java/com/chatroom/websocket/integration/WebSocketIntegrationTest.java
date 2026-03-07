package com.chatroom.websocket.integration;

import com.chatroom.message.entity.ChatMessage;
import com.chatroom.message.service.MessageWriteService;
import com.chatroom.room.service.RoomService;
import com.chatroom.security.component.JwtProvider;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.websocket.component.SessionManager;
import com.chatroom.websocket.dto.WsMessageRequest;
import com.chatroom.websocket.enums.MessageType;
import com.chatroom.websocket.enums.WsAckCode;
import com.chatroom.websocket.enums.WsResponseType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoSpyBean
    private JwtProvider jwtProvider;
    @Autowired
    private SessionManager sessionManager;
    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private MessageWriteService messageWriteService;

    @Test
    void should_connect_fail_when_token_invalid() {
        String token = "invalid_token";
        TestClientHandler handler = new TestClientHandler(0);
        try {
            WebSocketSession session = connect(token, "room1", handler);
            session.close();
            throw new AssertionError("Should not connect successfully");
        } catch (Exception e) {
            assertThat(e).isInstanceOfAny(
                    HandshakeFailureException.class,
                    ExecutionException.class
            );
        }
    }
    @Test
    void should_connect_fail_when_token_cannot_be_resolved() throws JOSEException, ParseException {
        String token = jwtProvider.generateJwt(new SecurityUser("user1", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        doThrow(new JOSEException())
                .when(jwtProvider).getAuthentication(token);
        TestClientHandler handler = new TestClientHandler(0);
        try {
            WebSocketSession session = connect(token, "room1", handler);
            session.close();
        } catch (Exception e){
            assertThat(e).isInstanceOfAny(
                    HandshakeFailureException.class,
                    ExecutionException.class
            );
        }
    }
    @Test
    void should_clear_session_context_when_session_closed() throws Exception {
        when(roomService.roomExists("room1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user1")).thenReturn(true);
        String token = generateTokenFor("user1");
        TestClientHandler handler = new TestClientHandler(1);
        WebSocketSession session = connect(token, "room1", handler);
        assertThat(sessionManager.getUserSessionsId("user1")).hasSize(1);
        session.close();
        await().untilAsserted(() -> assertThat(sessionManager.getAllSessionContext()).isEmpty());
    }
    @Test
    void should_join_room_successfully() throws Exception {
        when(roomService.roomExists("room1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user1")).thenReturn(true);

        String token = generateTokenFor("user1");

        TestClientHandler handler = new TestClientHandler(1);
        WebSocketSession session = connect(token, "room1", handler);

        WsMessageRequest request = new WsMessageRequest(
                MessageType.JOIN,
                "room1",
                "test",
                "req-1"
        );
        String json = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(json));

        handler.await();

        System.out.println("TEST MANAGER: " + System.identityHashCode(sessionManager));

        assertThat(handler.getReceived()).hasSize(1);
        assertThat(handler.getReceived().get(0))
                .contains("Join into room");
    }

    @Test
    void should_broadcast_textmessage_to_room() throws Exception {
        when(roomService.roomExists("room1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user2")).thenReturn(true);

        ChatMessage chatMessage = new ChatMessage("123456-0", "user1", "room1",
                com.chatroom.message.domain.MessageType.TEXT,
                "hello",
                Instant.now().toEpochMilli());
        when(messageWriteService.saveMessage(any(), any(), any(), any()))
                .thenReturn(chatMessage);

        String token1 = generateTokenFor("user1");
        String token2 = generateTokenFor("user2");

        TestClientHandler handler1 = new TestClientHandler(2); // ACK + 广播
        TestClientHandler handler2 = new TestClientHandler(1); // 广播

        WebSocketSession s1 = connect(token1, "room1", handler1);
        WebSocketSession s2 = connect(token2, "room1", handler2);

        joinRoom(s1, "room1", "req-1");
        joinRoom(s2, "room1", "req-2");

        Thread.sleep(1000); // 等待 join 完成

        // 发送 TEXT
        WsMessageRequest textReq = new WsMessageRequest(
                MessageType.TEXT,
                "room1",
                "hello",
                "req-3"
        );

        s1.sendMessage(new TextMessage(
                new ObjectMapper().writeValueAsString(textReq)
        ));

        handler1.await();
        handler2.await();

        // 断言 user1 收到 ACK
        assertThat(handler1.getReceived())
                .anyMatch(msg -> msg.contains("OK"));

        // 断言 user2 收到广播
        assertThat(handler2.getReceived())
                .anyMatch(msg -> msg.contains("hello"));

        // 验证 roomContext
        assertThat(sessionManager.getRoomSessionsId("room1"))
                .hasSize(2);
    }
    @Test
    void should_receive_ack_after_heartbeat() throws Exception {
        when(roomService.roomExists("room1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user1")).thenReturn(true);
        String token = generateTokenFor("user1");
        TestClientHandler handler = new TestClientHandler(1);
        WebSocketSession session = connect(token, "room1", handler);
        WsMessageRequest textReq = new WsMessageRequest(
                MessageType.HEARTBEAT,
                "room1",
                null,
                "req-1"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(textReq)));
        handler.await();
        assertThat(handler.getReceived())
                .anyMatch(msg -> msg.contains(WsResponseType.ACK.name()));
    }
    @Test
    void should_receive_ack_after_leave() throws Exception {
        when(roomService.roomExists("room1")).thenReturn(true);
        when(roomService.authorizeRoomAccess("room1", "user1")).thenReturn(true);
        String token = generateTokenFor("user1");
        TestClientHandler handler = new TestClientHandler(1);
        WebSocketSession session = connect(token, "room1", handler);
        joinRoom(session, "room1", "req-1");
        handler.await();
        await().untilAsserted(() ->
                assertThat(handler.getReceived().get(0)).contains("Join into room")
        );
        WsMessageRequest leaveReq = new WsMessageRequest(
                MessageType.LEAVE,
                "room1",
                null,
                "req-2"
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(leaveReq)));
        handler.await();
        assertThat(handler.getReceived())
                .anyMatch(msg -> msg.contains(WsAckCode.LEAVE_ROOM_SUCCESS.name()));
    }
    private WebSocketSession connect(String token, String roomId, TestClientHandler handler) throws Exception{
        WebSocketClient client = new StandardWebSocketClient();

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add("Authorization", "Bearer " + token);

        URI uri = new URI("ws://localhost:" + port + "/ws/chat?roomId=" + roomId);

        return client.execute(handler, headers, uri).get(5, TimeUnit.SECONDS);
    }
    private String generateTokenFor(String username) throws JOSEException {
        return jwtProvider.generateJwt(new SecurityUser(username, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
    private void joinRoom(WebSocketSession session, String roomId, String reqId) throws IOException {
        WsMessageRequest request = new WsMessageRequest(
                MessageType.JOIN,
                roomId,
                "test",
                reqId
        );
        String json = objectMapper.writeValueAsString(request);
        session.sendMessage(new TextMessage(json));
    }

    class TestClientHandler extends TextWebSocketHandler {
        private List<String> receive = new CopyOnWriteArrayList<>();
        private CountDownLatch latch;
        TestClientHandler(int expectCountDownLatch){
            latch = new CountDownLatch(expectCountDownLatch);
        }
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            receive.add(message.getPayload());
            latch.countDown();
        }
        public void await() throws InterruptedException {
            boolean ok = latch.await(10, TimeUnit.SECONDS);
            if (!ok) {
                throw new AssertionError("Did not receive message in time");
            }
        }
        public List<String> getReceived() {
            return receive;
        }
    }
}
