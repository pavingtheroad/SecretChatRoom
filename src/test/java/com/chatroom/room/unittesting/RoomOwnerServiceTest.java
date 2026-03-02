package com.chatroom.room.unittesting;

import com.chatroom.component.UserIdentityResolver;
import com.chatroom.room.component.RoomAuthorization;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.room.service.RoomOwnerServiceImpl;
import com.chatroom.security.component.IdentityResolver;
import com.chatroom.user.exception.UserNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoomOwnerServiceTest {
    @Mock
    private RoomCacheRepository roomCacheRepository;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private RoomAuthorization roomAuthorization;

    @InjectMocks
    private RoomOwnerServiceImpl roomOwnerService;
    private MockedStatic<IdentityResolver> mockedIdentityResolver;
    private SecurityContext securityContext;
    @BeforeEach
    void setUp() {
        // Mock静态方法
        mockedIdentityResolver = mockStatic(IdentityResolver.class);

        // 设置Spring Security上下文
        securityContext = SecurityContextHolder.createEmptyContext();
        SecurityContextHolder.setContext(securityContext);
    }
    @AfterEach
    void tearDown() {
        // 清理静态方法mock
        if (mockedIdentityResolver != null) {
            mockedIdentityResolver.close();
        }
        // 清理安全上下文
    }
    @Test
    void success_addUserToRoom() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        giveUserPKId_userIdentityResolver(userId);
        when(roomCacheRepository.addUserToRoom(eq(roomId), any())).thenReturn(1L);
        roomOwnerService.addUserToRoom(roomId, userId);
        verify(roomCacheRepository, times(1))
                .addUserToRoom(roomId, String.valueOf(10001L));
    }
    @Test
    void accessDeniedException_addUserToRoom() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        throwsException_IdentityResolver();
        Exception exception = assertThrows(
                AccessDeniedException.class,
                () -> roomOwnerService.addUserToRoom(roomId, userId)
        );
    }
    @Test
    void roomAuthorityException_addUserToRoom() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        giveOperatorPKId_IdentityResolver();
        throwsException_checkRoomOwner(roomId, "10001");
        Exception exception = assertThrows(
                RoomAuthorityException.class,
                () -> roomOwnerService.addUserToRoom(roomId, userId)
        );
    }
    @Test
    void userNotFoundException_addUserToRoom() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        throwsException_userIdentityResolver();
        Exception exception = assertThrows(
                UserNotFoundException.class,
                () -> roomOwnerService.addUserToRoom(roomId, userId)
        );
    }
    @Test
    void roomNotFoundException_addUserToRoom() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        giveUserPKId_userIdentityResolver(userId);
        throwsRoomNotFoundException_addUserToRoom(roomId);
        Exception exception = assertThrows(
                RoomNotFoundException.class,
                () -> roomOwnerService.addUserToRoom(roomId, userId)
        );
    }
    @Test
    void success_manageRoomInfo() throws Exception {
        String roomId = "roomId";
        RoomInfoUpdate roomInfo = RoomInfoUpdate.empty();
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        doNothing().when(roomCacheRepository).updateRoomInfo(roomId, roomInfo);
        roomOwnerService.manageRoomInfo(roomId, roomInfo);
    }
    @Test
    void roomNotFoundException_manageRoomInfo() throws Exception {
        String roomId = "roomId";
        RoomInfoUpdate roomInfo = RoomInfoUpdate.empty();
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        doThrow(RoomNotFoundException.class)
                .when(roomCacheRepository).updateRoomInfo(roomId, roomInfo);
        Exception exception = assertThrows(
                RoomNotFoundException.class,
                () -> roomOwnerService.manageRoomInfo(roomId, roomInfo)
        );
    }
    @Test
    void success_deleteRoom() throws Exception {
        String roomId = "roomId";
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        when(roomCacheRepository.deleteRoom(roomId))
                .thenReturn(true);
        roomOwnerService.deleteRoom(roomId);
    }
    @Test
    void roomNotFoundException_deleteRoom() throws Exception {
        String roomId = "roomId";
        giveOperatorPKId_IdentityResolver();
        access_checkRoomOwner(roomId);
        doThrow(RoomNotFoundException.class)
                .when(roomCacheRepository).deleteRoom(roomId);
        Exception exception = assertThrows(
                RoomNotFoundException.class,
                () -> roomOwnerService.deleteRoom(roomId)
        );
    }
    @Test
    void success_putEncryptedKey() throws Exception {
        String roomId = "roomId";
        String userId = "userId";
        String encryptedKey = "encryptedKey";
        giveOperatorPKId_IdentityResolver();
        doNothing().when(roomCacheRepository).putEncryptedRoomKey(eq(roomId), any(), eq(encryptedKey));
        roomCacheRepository.putEncryptedRoomKey(roomId, userId, encryptedKey);
    }


    private void giveOperatorPKId_IdentityResolver() {
        mockedIdentityResolver.when(IdentityResolver::currentUserPKId)
                .thenReturn(10001L);
    }
    private void throwsException_IdentityResolver() {
        mockedIdentityResolver.when(IdentityResolver::currentUserPKId)
                .thenThrow(AccessDeniedException.class);
    }
    private void access_checkRoomOwner(String roomId){
        doNothing().when(roomAuthorization).checkRoomOwner(eq(roomId), any());
    }
    private void throwsException_checkRoomOwner(String roomId, String operatorPKId){
        doThrow(RoomAuthorityException.class)
                .when(roomAuthorization)
                .checkRoomOwner(roomId, operatorPKId);
    }
    private void giveUserPKId_userIdentityResolver(String userId){
        when(userIdentityResolver.getUserPKIdByUserId(userId))
                .thenReturn(10001L);
    }
    private void throwsException_userIdentityResolver(){
        doThrow(UserNotFoundException.class)
                .when(userIdentityResolver)
                .getUserPKIdByUserId(any());
    }

    private void throwsRoomNotFoundException_addUserToRoom(String roomId){
        doThrow(RoomNotFoundException.class)
                .when(roomCacheRepository)
                .addUserToRoom(eq(roomId), any());
    }
}
