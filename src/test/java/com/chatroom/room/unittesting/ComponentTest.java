package com.chatroom.room.unittesting;

import com.chatroom.room.component.RoomAuthorization;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.exception.RoomAuthorityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ComponentTest {
    @Mock
    private RoomCacheRepository roomCacheRepository;
    @InjectMocks
    private RoomAuthorization roomAuthorization;
    @Test
    void throwException_whenUserIsNotRoomOwner_checkRoomOwner() {
        String roomId = "roomId";
        String userPKId = "userPKId";
        when(roomCacheRepository.isRoomOwner(roomId, userPKId))
                .thenReturn(false);
        RoomAuthorityException exception = assertThrows(
                RoomAuthorityException.class,
                () -> roomAuthorization.checkRoomOwner(roomId, userPKId)
        );
        assertEquals("WRONG_AUTHORITY_TO_OPERATE_ROOM", exception.getErrorCode());
    }
    @Test
    void doNothing_whenUserIsRoomOwner_checkRoomOwner() {
        String roomId = "roomId";
        String userPKId = "userPKId";
        when(roomCacheRepository.isRoomOwner(roomId, userPKId))
                .thenReturn(true);
        assertDoesNotThrow(() -> roomAuthorization.checkRoomOwner(roomId, userPKId));
    }
}
