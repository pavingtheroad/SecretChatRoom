package com.chatroom.user.service;

import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.dto.UserRegisterDTO;
import com.chatroom.user.entity.UserEntity;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.EmailOccupiedException;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = EmailOccupiedException.class)
    public void registerUser(UserRegisterDTO userRegisterDTO){
        String encodedPassword = passwordEncoder.encode(userRegisterDTO.password());
        UserEntity userEntity = UserRegisterDTO.toEntity(userRegisterDTO, encodedPassword);
        userMapper.insertUser(userEntity);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = UserNotFoundException.class)
    public UserInfoDTO getUserById(String userId){
        return userMapper.selectByUserId(userId).toUserInfoDTO();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = UserNotFoundException.class)
    public List<UserInfoDTO> searchUserByName(String userName){
        return userMapper.selectByUserName(userName).stream()
                .map(UserEntity::toUserInfoDTO)
                .toList();
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void cancelUser(String targetUserId, String operatorUserId, UserStatus status) throws AuthorityException, UserNotFoundException {
        /**
         * 鉴定用户权限是否为admin
         */
        if (!operatorUserId.equals(targetUserId)) {
            List<String> roles = userMapper.getUserRoles(operatorUserId);
            if (!roles.contains("ADMIN")) {
                throw new AuthorityException(operatorUserId);
            }
        }
        int updated = userMapper.updateUserStatus(targetUserId, UserStatus.DELETED);
        if (updated == 0) {
            throw new UserNotFoundException(targetUserId);
        }
    }
}
