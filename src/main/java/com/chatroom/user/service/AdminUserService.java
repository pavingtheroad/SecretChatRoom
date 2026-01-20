package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final RoleService rs;
    private final UserService us;
    private final UserRoleService urs;
    public AdminUserService(RoleService roleService,  UserService userService, UserRoleService userRoleService) {
        this.rs = roleService;
        this.us = userService;
        this.urs = userRoleService;
    }

    private static Logger logger = LoggerFactory.getLogger(AdminUserService.class);
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void insertRoleToUser(String userId, String roleCode) throws DuplicateKeyException, UserNotFoundException{
        /**
         * 鉴定用户权限是否为admin
         */
        Long roleId = rs.getRoleIdByCode(roleCode);
        Long userPKId = us.transformUserIdToPKId(userId);
        urs.insertRoleToUser(userPKId, roleId);
    }

}
