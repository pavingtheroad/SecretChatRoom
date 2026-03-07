package com.chatroom.message.slicetesting;

import com.chatroom.message.controller.MessageQueryController;
import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.service.MessageQueryService;
import com.chatroom.room.service.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


@WebMvcTest(
        controllers = MessageQueryController.class
)
public class MessageQueryControllerTest {
    @Autowired
    private MockMvc mockMvc;    // 发送模拟HTTP请求
    @MockitoBean
    private MessageQueryService messageQueryService;
    @MockitoBean
    private RoomService roomService;
    // init查询
    @Test
    void shouldReturn401_whenUserNotAuthorized_initQuery() throws Exception {
        givenRoomAuthorized(false);
        mockMvc.perform(get("/rooms/room1/messages/init")
                    .param("limit", "10"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldReturnMessages_whenUserAuthorized_initQuery() throws Exception {
        givenRoomAuthorized(true);
        when(messageQueryService.initMessageQuery(
                eq("room1"),
                eq("userPKId"),
                anyInt()
        )).thenReturn(List.of(buildMessageDTO("user1", "text", "hello", "")));
        mockMvc.perform(get("/rooms/room1/messages/init")
                        .param("limit", "10")
                        .with(user("user1")))
                .andExpect(status().isOk());
        verify(messageQueryService)
                .initMessageQuery("room1", "user1", 10);
    }
    @Test
    @WithMockUser(username = "user1")
    void shouldReturn401_whenUserNotAuthorized_getEarlierMessages() throws Exception {
        givenRoomAuthorized(false);
        mockMvc.perform(get("/rooms/room1/messages/earlier")
                    .param("start", "123-1")
                    .param("limit", "10"))
                .andExpect(status().isUnauthorized());
        verify(messageQueryService, never())
                .getForwardMessages(any(), any(), any(), anyInt());
    }
    @Test
    @WithMockUser(username = "user1")
    void shouldReturnMessages_whenUserAuthorized_getEarlierMessages() throws Exception {
        givenRoomAuthorized(true);
        when(messageQueryService.getForwardMessages(eq("room1"), eq("user1"), any(), anyInt()))
                .thenReturn(List.of(buildMessageDTO("user1", "text", "hello", "")));
        mockMvc.perform(get("/rooms/room1/messages/earlier")
                    .param("start", "123-1")
                    .param("limit", "10")
                    )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
        verify(messageQueryService)
                .getForwardMessages("room1", "user1", "123-1", 10);
    }
    @Test
    void shouldReturn400_whenInvalidStreamId_getEarlierMessages() throws Exception {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("start", "invalid");
        params.add("limit", "10");
        mockMvc.perform(get("/rooms/room1/messages/earlier")
                    .params(params)
                    .with(user("user1")))
                .andExpect(status().isBadRequest());
    }
    // later查询
    @Test
    void shouldReturn401_whenUserNotAuthorized_getLaterMessages() throws Exception {
        givenRoomAuthorized(false);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("end", "123-1");
        params.add("limit", "10");
        mockMvc.perform(get("/rooms/room1/messages/later")
                    .params(params))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldReturnMessages_whenUserAuthorized_getLaterMessages() throws Exception {
        givenRoomAuthorized(true);
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("end", "123-1");
        when(messageQueryService.getBackwardMessages(eq("room1"), eq("user1"), any(), anyInt()))
                .thenReturn(List.of(buildMessageDTO("user1", "text", "hello", "")));
        mockMvc.perform(get("/rooms/room1/messages/later")
                .params(params)
                .with(user("user1")))
                .andExpect(status().isOk());
        verify(messageQueryService)
                .getBackwardMessages("room1", "user1", "123-1", 10);
    }
    private void givenRoomAuthorized(boolean value){
        when(roomService.authorizeRoomAccess("room1", "user1"))
                .thenReturn(value);
    }
    private MessageDTO buildMessageDTO(String senderId, String type, String content, String createdAt){
        return new MessageDTO(senderId, type, content, createdAt);
    }
}
