package com.chatroom.room.unittesting;

import com.chatroom.component.UserIdentityResolver;
import com.chatroom.message.dao.RoomStateRepository;
import com.chatroom.room.component.RoomAuthorization;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.room.exception.UserNotInRoomException;
import com.chatroom.room.service.RoomServiceImpl;
import com.chatroom.user.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomServiceTest {
    @Mock
    private RoomCacheRepository roomCacheRepository;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private RoomAuthorization roomAuthorization;
    @Mock
    private RoomStateRepository roomStateRepository;

    private RoomServiceImpl service;
    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(
                roomCacheRepository,
                userIdentityResolver,
                roomAuthorization,
                roomStateRepository
        );
    }
    // ==================
    @Test
    void createRoom_success() {
        RoomInfo info = new RoomInfo("r1", "name", "owner", null, 1L, false, false, 0L);

        service.createRoom(info);

        verify(roomCacheRepository).createRoom(info);
    }

    @Test
    void createRoom_alreadyExists() {
        RoomInfo info = new RoomInfo("r1", "name", "owner", null, 1L, false, false, 0L);

        doThrow(new RoomAlreadyExistsException("r1"))
                .when(roomCacheRepository)
                .createRoom(info);

        assertThatThrownBy(() -> service.createRoom(info))
                .isInstanceOf(RoomAlreadyExistsException.class);
    }
    // =================
    @Test
    void getRoomInfo_success() {
        RoomInfo info = new RoomInfo("r1", "name", "owner", null, 1L, false, false, 0L);
        when(roomCacheRepository.getRoomInfo("r1")).thenReturn(info);

        RoomInfo result = service.getRoomInfo("r1");

        assertThat(result).isSameAs(info);
    }

    @Test
    void getRoomInfo_notFound() {
        when(roomCacheRepository.getRoomInfo("r1"))
                .thenThrow(new RoomNotFoundException("r1"));

        assertThatThrownBy(() -> service.getRoomInfo("r1"))
                .isInstanceOf(RoomNotFoundException.class);
    }
    // ==================
    @Test
    void joinRoom_success() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenReturn(10L);

        when(roomCacheRepository.addUserToRoom("r1", "10"))
                .thenReturn(1L);

        service.joinRoom("r1", "u1");

        verify(roomCacheRepository).addUserToRoom("r1", "10");
    }

    @Test
    void joinRoom_userNotFound() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenThrow(new UserNotFoundException("u1"));

        assertThatThrownBy(() -> service.joinRoom("r1", "u1"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void joinRoom_roomLocked() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenReturn(10L);

        when(roomCacheRepository.addUserToRoom("r1", "10"))
                .thenReturn(-2L);

        assertThatThrownBy(() -> service.joinRoom("r1", "u1"))
                .isInstanceOf(RoomAuthorityException.class);
    }

    @Test
    void joinRoom_roomNotFound() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenReturn(10L);

        when(roomCacheRepository.addUserToRoom("r1", "10"))
                .thenThrow(new RoomNotFoundException("r1"));

        assertThatThrownBy(() -> service.joinRoom("r1", "u1"))
                .isInstanceOf(RoomNotFoundException.class);
    }
    // ==================
    @Test
    void joinedRoomsId_success() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenReturn(10L);

        when(roomCacheRepository.joinedRooms("10"))
                .thenReturn(Set.of("r1", "r2"));

        Set<String> result = service.joinedRoomsId("u1");

        assertThat(result).containsExactlyInAnyOrder("r1", "r2");
    }

    @Test
    void joinedRoomsId_userNotFound() {
        when(userIdentityResolver.getUserPKIdByUserId("u1"))
                .thenThrow(new UserNotFoundException("u1"));

        assertThatThrownBy(() -> service.joinedRoomsId("u1"))
                .isInstanceOf(UserNotFoundException.class);
    }
    // ==================
    @Test
    void leaveRoom_singleMember_deleteRoom() {
        when(userIdentityResolver.getUserPKIdByUserId("u1")).thenReturn(10L);
        when(userIdentityResolver.getUserPKIdByUserId("op")).thenReturn(20L);

        when(roomCacheRepository.getRoomMembersId("r1"))
                .thenReturn(Set.of("10"));

        service.leaveRoom("r1", "u1", "op");

        verify(roomCacheRepository).deleteRoom("r1");
        verify(roomAuthorization, never()).authorizeLeaveRoom(any(), any(), any());
    }

    @Test
    void leaveRoom_userNotInRoom() {
        when(userIdentityResolver.getUserPKIdByUserId("u1")).thenReturn(10L);
        when(userIdentityResolver.getUserPKIdByUserId("op")).thenReturn(20L);

        when(roomCacheRepository.getRoomMembersId("r1"))
                .thenReturn(Set.of("u2", "u3"));

        assertThatThrownBy(() -> service.leaveRoom("r1", "u1", "op"))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    void leaveRoom_success() {
        when(userIdentityResolver.getUserPKIdByUserId("u1")).thenReturn(10L);
        when(userIdentityResolver.getUserPKIdByUserId("op")).thenReturn(20L);

        when(roomCacheRepository.getRoomMembersId("r1"))
                .thenReturn(Set.of("10", "20"));

        service.leaveRoom("r1", "u1", "op");

        verify(roomAuthorization).authorizeLeaveRoom("r1", "10", "20");
        verify(roomCacheRepository).removeUserFromRoom("r1", "10");
    }

    @Test
    void leaveRoom_authorizationFails() {
        when(userIdentityResolver.getUserPKIdByUserId("u1")).thenReturn(10L);
        when(userIdentityResolver.getUserPKIdByUserId("op")).thenReturn(20L);

        when(roomCacheRepository.getRoomMembersId("r1"))
                .thenReturn(Set.of("10", "20"));

        doThrow(new RoomAuthorityException("r1"))
                .when(roomAuthorization)
                .authorizeLeaveRoom("r1", "10", "20");

        assertThatThrownBy(() -> service.leaveRoom("r1", "u1", "op"))
                .isInstanceOf(RoomAuthorityException.class);
    }

    @Test
    void leaveRoom_removeUserThrows() {
        when(userIdentityResolver.getUserPKIdByUserId("u1")).thenReturn(10L);
        when(userIdentityResolver.getUserPKIdByUserId("op")).thenReturn(20L);

        when(roomCacheRepository.getRoomMembersId("r1"))
                .thenReturn(Set.of("10", "20"));

        doThrow(new RoomNotFoundException("r1"))
                .when(roomCacheRepository)
                .removeUserFromRoom("r1", "10");

        assertThatThrownBy(() -> service.leaveRoom("r1", "u1", "op"))
                .isInstanceOf(RoomNotFoundException.class);
    }
    // ====================
    @Test
    void authorizeRoomAccess_true() {
        when(roomCacheRepository.authorizeRoomAccess("r1", "10"))
                .thenReturn(true);

        assertThat(service.authorizeRoomAccess("r1", "10")).isTrue();
    }

    @Test
    void authorizeRoomAccess_false() {
        when(roomCacheRepository.authorizeRoomAccess("r1", "10"))
                .thenReturn(false);

        assertThat(service.authorizeRoomAccess("r1", "10")).isFalse();
    }

    @Test
    void authorizeRoomAccess_roomNotFound() {
        when(roomCacheRepository.authorizeRoomAccess("r1", "10"))
                .thenThrow(new RoomNotFoundException("r1"));

        assertThatThrownBy(() -> service.authorizeRoomAccess("r1", "10"))
                .isInstanceOf(RoomNotFoundException.class);
    }
    // =========================
    @Test
    void roomExists_true() {
        when(roomCacheRepository.roomExists("r1")).thenReturn(true);
        assertThat(service.roomExists("r1")).isTrue();
    }

    @Test
    void roomExists_false() {
        when(roomCacheRepository.roomExists("r1")).thenReturn(false);
        assertThat(service.roomExists("r1")).isFalse();
    }
    // ==========================
    @Test
    void getEncryptedKey_success() {
        when(roomCacheRepository.getEncryptedRoomKey("r1", "10"))
                .thenReturn("encrypted");

        assertThat(service.getEncryptedKey("r1", "10"))
                .isEqualTo("encrypted");
    }
}
