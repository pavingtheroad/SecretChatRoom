package com.chatroom.user.service;

import com.chatroom.component.UserIdentityResolver;
import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.dto.UserInfoUpdate;
import com.chatroom.user.dto.UserProfile;
import com.chatroom.user.dto.UserRegisterDTO;
import com.chatroom.user.entity.UserEntity;
import com.chatroom.user.exception.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_success(){
        UserRegisterDTO dto = new UserRegisterDTO("test",
                "123456",
                "test@test.com",
                "avatar.jpg");
        String encodedPassword = "encodedPassword";
        when(passwordEncoder.encode("123456"))
                .thenReturn(encodedPassword);
        userService.registerUser(dto);
        verify(passwordEncoder).encode("123456");
        verify(userMapper).insertUser(any());
    }
    @Test
    void registerUser_emailOccupied(){
        UserRegisterDTO dto = new UserRegisterDTO("test",
                "123456",
                "existing@example.com",
                "avatar.jpg");
        String encodedPassword = "encodedPassword";
        when(passwordEncoder.encode("123456"))
                .thenReturn(encodedPassword);
        doThrow(new DuplicateKeyException("Email already exists"))
                .when(userMapper).insertUser(any());
        EmailOccupiedException exception = assertThrows(
                EmailOccupiedException.class,
                () -> userService.registerUser(dto),
                "应该抛出 EmailOccupiedException"
        );
        assertEquals("existing@example.com", exception.getMessage().split(":")[1].trim());
        verify(passwordEncoder).encode("123456");
        verify(userMapper).insertUser(any());
    }
    @Test
    void getUserById_success(){
        String userId = "test";
        UserEntity userEntity = new UserEntity(1001L, userId, "userName", null, "password", null, UserStatus.ACTIVE);
        givenUserById(userId, userEntity);
        UserInfoDTO result = userService.getUserById(userId);
        assertEquals(userId, result.userId());
    }
    @Test
    void getUserById_throwsException_whenUserNotFound(){
        String userId = "test";
        givenUserById(userId, null);
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(userId)
        );
    }
    @Test
    void getUserById_throwsException_whenUserCanceled(){
        String userId = "test";
        UserEntity userEntity = new UserEntity(1001L, userId, null, null, null, null, UserStatus.DELETED);
        givenUserById(userId, userEntity);
        UserCanceledException exception = assertThrows(
                UserCanceledException.class,
                () -> userService.getUserById(userId)
        );
    }
    @Test
    void getUserProfile_success(){
        String userId = "test";
        UserEntity userEntity = new UserEntity(1001L, userId, "userName", null, null, null, UserStatus.ACTIVE);
        when(userMapper.selectUserByUserId(userId)).thenReturn(userEntity);

        UserProfile result = userService.getUserProfileByUserId(userId);

        assertEquals(userId, result.userId());
        assertEquals("userName", result.userName());
    }
    @Test
    void getUserProfile_throwsException_whenUserNotFound(){
        String userId = "nonexistent";
        when(userMapper.selectUserByUserId(userId)).thenReturn(null);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserProfileByUserId(userId)
        );
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }
    @Test
    void cancelUser_selfCancel_success(){
        String targetUserId = "1001";
        String operatorUserId = "1001"; // 自己注销自己
        Long targetPKId = 1001L;

        givenSelectPKIdByUserId(targetUserId, targetPKId);
        when(userMapper.updateUserStatus(targetPKId, UserStatus.DELETED)).thenReturn(1);

        Long result = userService.cancelUser(targetUserId, operatorUserId);

        assertEquals(targetPKId, result);
        verify(userMapper).updateUserStatus(targetPKId, UserStatus.DELETED);
    }
    @Test
    void cancelUser_adminCancel_success(){
        String targetUserId = "1001";
        String operatorUserId = "1002"; // 管理员操作
        Long targetPKId = 1001L;
        Long operatorPKId = 1002L;

        givenSelectPKIdByUserId(targetUserId, targetPKId);
        when(userMapper.getUserRoles(operatorPKId)).thenReturn(Arrays.asList("ADMIN"));
        when(userMapper.updateUserStatus(targetPKId, UserStatus.DELETED)).thenReturn(1);

        Long result = userService.cancelUser(targetUserId, operatorUserId);

        assertEquals(targetPKId, result);
        verify(userMapper).getUserRoles(operatorPKId);
        verify(userMapper).updateUserStatus(targetPKId, UserStatus.DELETED);
    }
    @Test
    void cancelUser_throwsAuthorityException_whenNonAdminOperatesOtherUser(){
        String targetUserId = "1001";
        String operatorUserId = "1002"; // 普通用户尝试注销其他用户
        Long targetPKId = 1001L;
        Long operatorPKId = 1002L;

        givenSelectPKIdByUserId(targetUserId, targetPKId);
        when(userMapper.getUserRoles(operatorPKId)).thenReturn(Arrays.asList("USER")); // 非管理员

        AuthorityException exception = assertThrows(
                AuthorityException.class,
                () -> userService.cancelUser(targetUserId, operatorUserId)
        );
        assertEquals(operatorUserId, exception.getMessage().split(":")[1].trim());
    }
    @Test
    void cancelUser_throwsUserNotFoundException_whenUserNotExists(){
        String targetUserId = "9999";
        String operatorUserId = "9999";
        Long targetPKId = 9999L;

        givenSelectPKIdByUserId(targetUserId, targetPKId);
        when(userMapper.updateUserStatus(targetPKId, UserStatus.DELETED)).thenReturn(0);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.cancelUser(targetUserId, operatorUserId)
        );
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }
    @Test
    void updateUserInfo_success(){
        UserInfoUpdate updateDto = new UserInfoUpdate("1001", "newName", "newAvatar", "newEmail");
        String operatorUserId = "1001";
        Long operatorPKId = 1001L;

        when(userMapper.updateUserInfo(updateDto, operatorPKId)).thenReturn(1);

        userService.updateUserInfo(updateDto, operatorUserId);

        verify(userMapper).updateUserInfo(updateDto, operatorPKId);
    }

    @Test
    void updateUserInfo_throwsUserException_whenUpdateFails(){
        UserInfoUpdate updateDto = new UserInfoUpdate("1001", "newName", "newAvatar", "newEmail");
        String operatorUserId = "1001";
        Long operatorPKId = 1001L;

        when(userMapper.updateUserInfo(updateDto, operatorPKId)).thenReturn(0);

        UserException exception = assertThrows(
                UserException.class,
                () -> userService.updateUserInfo(updateDto, operatorUserId)
        );
        assertEquals("USER_INFO_UPDATED_FAILED", exception.getErrorCode());
    }

    @Test
    void updateUserInfo_throwsUserException_whenUserIdInvalid(){
        UserInfoUpdate updateDto = new UserInfoUpdate("1001", "newName", "newAvatar", "newEmail");
        String operatorUserId = "-1"; // 无效的用户ID

        UserException exception = assertThrows(
                UserException.class,
                () -> userService.updateUserInfo(updateDto, operatorUserId)
        );
        assertEquals("USER_ID_LOST_CONNECTION", exception.getErrorCode());
    }
    @Test
    void updateUserStatus_success(){
        Long targetUserPKId = 1001L;
        UserStatus newStatus = UserStatus.BANNED;

        when(userMapper.updateUserStatus(targetUserPKId, newStatus)).thenReturn(1);

        userService.updateUserStatus(targetUserPKId, newStatus);

        verify(userMapper).updateUserStatus(targetUserPKId, newStatus);
    }

    @Test
    void updateUserStatus_throwsUserNotFoundException_whenUserNotExists(){
        Long targetUserPKId = 9999L;
        UserStatus newStatus = UserStatus.BANNED;

        when(userMapper.updateUserStatus(targetUserPKId, newStatus)).thenReturn(0);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUserStatus(targetUserPKId, newStatus)
        );
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }
    @Test
    void registerPublicKey_success(){
        Long userId = 1001L;
        String publicKey = "mockKey";

        userService.registerPublicKey(userId, publicKey);

        verify(userMapper, times(1))
                .insertPublicKey(userId, publicKey);
    }
    @Test
    void registerPublicKey_throwsDuplicateKeyException_whenKeyExists(){
        Long userId = 1001L;
        String publicKey = "duplicateKey";

        doThrow(new DuplicateKeyException("Public key already exists"))
                .when(userMapper).insertPublicKey(userId, publicKey);

        DuplicateKeyException exception = assertThrows(
                DuplicateKeyException.class,
                () -> userService.registerPublicKey(userId, publicKey)
        );
        assertEquals("Public key already exists", exception.getMessage());
    }
    @Test
    void getUserPublicKey_success(){
        Long targetUserId = 1001L;
        Long targetUserPKId = 1001L;
        String publicKey = "user_public_key";

        when(userIdentityResolver.getUserPKIdByUserId(String.valueOf(targetUserId))).thenReturn(targetUserPKId);
        when(userMapper.getPublicKey(targetUserPKId)).thenReturn(publicKey);

        String result = userService.getUserPublicKey(targetUserId);

        assertEquals(publicKey, result);
        verify(userIdentityResolver).getUserPKIdByUserId(String.valueOf(targetUserId));
        verify(userMapper).getPublicKey(targetUserPKId);
    }

    @Test
    void getUserPublicKey_throwsUserNotFoundException_whenUserNotExists(){
        Long targetUserId = 9999L;
        String targetUserIdStr = "9999";

        when(userIdentityResolver.getUserPKIdByUserId(targetUserIdStr))
                .thenThrow(new UserNotFoundException(targetUserIdStr));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserPublicKey(targetUserId)
        );
        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getUserPublicKey_throwsPublicKeyNotFoundException_whenKeyNotExists(){
        Long targetUserId = 1001L;
        Long targetUserPKId = 1001L;

        when(userIdentityResolver.getUserPKIdByUserId(String.valueOf(targetUserId))).thenReturn(targetUserPKId);
        when(userMapper.getPublicKey(targetUserPKId)).thenReturn(null);

        PublicKeyNotFoundException exception = assertThrows(
                PublicKeyNotFoundException.class,
                () -> userService.getUserPublicKey(targetUserId)
        );
        assertEquals("UNABLE_TO_GET_PUBLIC_KEY", exception.getErrorCode());
    }
    private void givenUserById(String userId, UserEntity userEntity){
        when(userMapper.selectUserByUserId(userId))
                .thenReturn(userEntity);
    }
    private void givenSelectPKIdByUserId(String userId, Long pkId){
        when(userMapper.selectPKIdByUserId(userId))
                .thenReturn(pkId);
    }

}
