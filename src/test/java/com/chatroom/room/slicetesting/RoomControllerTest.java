package com.chatroom.room.slicetesting;

import com.chatroom.exceptionHandler.GlobalExceptionHandler;
import com.chatroom.room.controller.AddUserRequest;
import com.chatroom.room.controller.RoomController;
import com.chatroom.room.dto.PutEncryptedKeyRequest;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.room.service.RoomOwnerService;
import com.chatroom.room.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RoomController.class
)
public class RoomControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private RoomService roomService;
    @MockitoBean
    private RoomOwnerService roomOwnerService;

    private static final String ROOM_ID = "room1";
    private static final String USER_ID = "user1";

    @Test
    void createRoom_success() throws Exception {
        RoomInfo request = RoomInfo.empty();

        mockMvc.perform(post("/room")
                        .with(user(USER_ID))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(roomService).createRoom(any());
    }

    @Test
    void createRoom_conflict() throws Exception {
        doThrow(new RoomAlreadyExistsException("r1"))
                .when(roomService).createRoom(any());

        mockMvc.perform(post("/room")
                        .with(user(USER_ID))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RoomInfo.empty())))
                .andExpect(status().isConflict());
    }
    // ==================
    @Test
    void joinRoom_success() throws Exception {
        when(roomService.getEncryptedKey(ROOM_ID, USER_ID))
                .thenReturn("encrypted");

        mockMvc.perform(post("/room/{roomId}/members/me", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("encrypted"));

        verify(roomService).joinRoom(ROOM_ID, USER_ID);
        verify(roomService).getEncryptedKey(ROOM_ID, USER_ID);
    }

    @Test
    void joinRoom_locked() throws Exception {
        doThrow(new RoomAuthorityException(ROOM_ID))
                .when(roomService).joinRoom(ROOM_ID, USER_ID);

        mockMvc.perform(post("/room/{roomId}/members/me", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
    // ==================
    @Test
    void getRoomInfo_success() throws Exception {
        when(roomService.getRoomInfo(ROOM_ID))
                .thenReturn(RoomInfo.empty());

        mockMvc.perform(get("/room/{roomId}/info", ROOM_ID)
                        .with(user(USER_ID)))
                .andExpect(status().isOk());
    }

    @Test
    void getRoomInfo_notFound() throws Exception {
        doThrow(new RoomNotFoundException(ROOM_ID))
                .when(roomService).getRoomInfo(ROOM_ID);

        mockMvc.perform(get("/room/{roomId}/info", ROOM_ID)
                        .with(user(USER_ID)))
                .andExpect(status().isNotFound());
    }
    // ================
    @Test
    void getRoomMembers_success() throws Exception {
        when(roomService.getRoomMembersId(ROOM_ID))
                .thenReturn(Set.of("u1", "u2"));

        mockMvc.perform(get("/room/{roomId}/members", ROOM_ID)
                        .with(user(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
    // ================
    @Test
    void getJoinedRooms_success() throws Exception {
        when(roomService.joinedRoomsId(USER_ID))
                .thenReturn(Set.of("r1"));

        mockMvc.perform(get("/room/joined")
                        .with(user(USER_ID)))
                .andExpect(status().isOk());

        verify(roomService).joinedRoomsId(USER_ID);
    }
    // ================
    @Test
    void leaveRoom_success() throws Exception {
        mockMvc.perform(delete("/room/leave/{roomId}", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(roomService).leaveRoom(ROOM_ID, USER_ID, USER_ID);
    }

    @Test
    void leaveRoom_forbidden() throws Exception {
        doThrow(new RoomAuthorityException(ROOM_ID))
                .when(roomService)
                .leaveRoom(ROOM_ID, USER_ID, USER_ID);

        mockMvc.perform(delete("/room/leave/{roomId}", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }
    // ===================
    @Test
    void addUserToRoom_success() throws Exception {
        AddUserRequest request = new AddUserRequest("targetUser");

        mockMvc.perform(post("/room/{roomId}/members", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomOwnerService).addUserToRoom(ROOM_ID, "targetUser");
    }
    // ===========================
    @Test
    void manageRoomInfo_success() throws Exception {
        mockMvc.perform(put("/room/{roomId}", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(RoomInfoUpdate.empty())))
                .andExpect(status().isOk());

        verify(roomOwnerService).manageRoomInfo(eq(ROOM_ID), any());
    }
    // =========================
    @Test
    void deleteRoom_success() throws Exception {
        mockMvc.perform(delete("/room/{roomId}", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(roomOwnerService).deleteRoom(ROOM_ID);
    }
    // ===========================
    @Test
    void putEncryptedKey_success() throws Exception {
        PutEncryptedKeyRequest request =
                new PutEncryptedKeyRequest("u2", "encrypted");

        mockMvc.perform(put("/room/{roomId}/encrypted-key", ROOM_ID)
                        .with(user(USER_ID))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(roomOwnerService)
                .putEncryptedKey(ROOM_ID, "u2", "encrypted");
    }
}
