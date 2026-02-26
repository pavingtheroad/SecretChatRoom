package com.chatroom.user.service;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.dto.UserInfoForAdmin;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUserServiceTest {
    @Mock
    private RoleService roleService;
    @Mock
    private UserService userService;
    @Mock
    private UserRoleService userRoleService;
    @InjectMocks
    private AdminUserService adminUserService;
    @Test
    void insertRoleToUser_success() {
        String userId = "userId";
        String roleCode = "ADMIN";
        when(roleService.getRoleIdByCode(roleCode))
                .thenReturn(101L);
        when(userService.transformUserIdToPKId(userId))
                .thenReturn(10001L);
        adminUserService.insertRoleToUser(userId, roleCode);
        verify(userRoleService).insertRoleToUser(10001L, 101L);
    }
    @Test
    void getUserInfo_success(){
        String userId = "userId";
        Long userPKId = 1001L;
        List<Long> roleIds = Arrays.asList(1L, 2L);
        List<String> roleCodes = Arrays.asList("USER", "MEMBER");

        // 准备测试数据
        UserInfoDTO userInfoDTO = new UserInfoDTO(userId, "testName", "avatar.jpg", UserStatus.ACTIVE);

        // 设置mock行为
        when(userService.transformUserIdToPKId(userId)).thenReturn(userPKId);
        when(userService.getUserById(userId)).thenReturn(userInfoDTO);
        when(userRoleService.getRoleIdsByUserId(userPKId)).thenReturn(roleIds);
        when(userRoleService.getRoleCodesByRoleIds(roleIds)).thenReturn(roleCodes);

        // 执行测试
        UserInfoForAdmin result = adminUserService.getUserInfo(userId);

        // 验证结果
        assertNotNull(result);
        assertEquals(userId, result.userId());
        assertEquals("testName", result.userName());
        assertEquals("avatar.jpg", result.avatarUrl());
        assertEquals(UserStatus.ACTIVE, result.status());
        assertEquals(2, result.roleCode().size());
        assertTrue(result.roleCode().contains("USER"));
        assertTrue(result.roleCode().contains("MEMBER"));

        // 验证方法调用
        verify(userService).transformUserIdToPKId(userId);
        verify(userService).getUserById(userId);
        verify(userRoleService).getRoleIdsByUserId(userPKId);
        verify(userRoleService).getRoleCodesByRoleIds(roleIds);
    }
    @Test
    void getUserInfo_throwsUserNotFoundException_whenUserNotExists(){
        String userId = "nonexistent";

        when(userService.transformUserIdToPKId(userId))
                .thenThrow(new UserNotFoundException(userId));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> adminUserService.getUserInfo(userId)
        );

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        verify(userService).transformUserIdToPKId(userId);
        verify(userService, never()).getUserById(anyString());
    }
    @Test
    void banUser_success() throws AuthorityException, UserNotFoundException, DuplicateKeyException {
        String userId = "testUser";
        String operatorUserId = "adminUser";
        Long userPKId = 1001L;

        // 设置mock行为
        when(userService.transformUserIdToPKId(userId)).thenReturn(userPKId);

        // 执行测试
        adminUserService.banUser(userId, operatorUserId);

        // 验证结果
        verify(userService).transformUserIdToPKId(userId);
        verify(userService).updateUserStatus(userPKId, UserStatus.BANNED);
    }

    @Test
    void banUser_throwsUserNotFoundException_whenUserNotExists()
            throws AuthorityException, UserNotFoundException, DuplicateKeyException {
        String userId = "nonexistent";
        String operatorUserId = "adminUser";

        when(userService.transformUserIdToPKId(userId))
                .thenThrow(new UserNotFoundException(userId));

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> adminUserService.banUser(userId, operatorUserId)
        );

        assertEquals("USER_NOT_FOUND", exception.getErrorCode());
        verify(userService).transformUserIdToPKId(userId);
        verify(userService, never()).updateUserStatus(anyLong(), any());
    }
}
