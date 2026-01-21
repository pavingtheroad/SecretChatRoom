package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserRoleService {
    private final UserMapper userMapper;
    public UserRoleService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public void insertRoleToUser(Long userPKId, Long roleId) {
        userMapper.insertUserRoleTable(userPKId, roleId);
    }
    public List<Long> getRoleIdsByUserId(Long userPKId){
        return userMapper.getRoleIdsByUserId(userPKId);
    }
    public List<String> getRoleCodesByRoleIds(List<Long> roleIds){
        List<String> roleCodes = new ArrayList<>();
        for(Long roleId : roleIds){
            roleCodes.add(userMapper.getRoleCodeById(roleId));
        }
        return roleCodes;
    }
}
