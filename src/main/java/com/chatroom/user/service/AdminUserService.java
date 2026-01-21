package com.chatroom.user.service;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.dto.UserInfoForAdmin;
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

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserInfoForAdmin getUserInfo(String userId){
        Long userPKId = us.transformUserIdToPKId(userId);
        UserInfoDTO userInfoDTO = us.getUserById(userId);
        List<String> roles = urs.getRoleCodesByRoleIds(urs.getRoleIdsByUserId(userPKId));
        return new UserInfoForAdmin(
                userInfoDTO.userId(),
                userInfoDTO.userName(),
                userInfoDTO.avatarUrl(),
                userInfoDTO.status(),
                roles
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void banUser(String userId, String operatorUserId) throws AuthorityException, UserNotFoundException, DuplicateKeyException{
        Long userPKId = us.transformUserIdToPKId(userId);
        Long operatorPKId = us.transformUserIdToPKId(operatorUserId);
        /**
         * 鉴定用户权限是否为admin
         */
        us.updateUserStatus(userPKId, UserStatus.BANNED);
    }

}
