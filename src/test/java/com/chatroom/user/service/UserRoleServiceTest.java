package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserRoleServiceTest {
    @Mock
    private UserMapper userMapper;
    @InjectMocks
    private UserRoleService userRoleService;
    @Test
    void getRoleCodesByRoleIds() {
        Long roleId = 1L;
        when(userMapper.getRoleCodeById(1L))
                .thenReturn("ADMIN");
        List<String> roleCodes = userRoleService.getRoleCodesByRoleIds(List.of(roleId));
        assertEquals("ADMIN", roleCodes.get(0));
    }
}
