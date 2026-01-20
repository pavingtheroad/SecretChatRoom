package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {
    private final UserMapper userMapper;
    public RoleService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    public Long getRoleIdByCode(String code){
        return userMapper.getRoleIdByCode(code);
    }
}
