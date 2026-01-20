package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRoleService {
    private final UserMapper userMapper;
    public UserRoleService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void insertRoleToUser(Long userPKId, Long roleId) {
        userMapper.insertUserRoleTable(userPKId, roleId);
    }
}
