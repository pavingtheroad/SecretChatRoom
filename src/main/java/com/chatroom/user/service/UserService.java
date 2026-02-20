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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserIdentityResolver userIdentityResolver;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder, UserIdentityResolver userIdentityResolver) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userIdentityResolver = userIdentityResolver;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void registerUser(UserRegisterDTO userRegisterDTO) throws EmailOccupiedException {
        // 未来添加邮箱验证逻辑，向注册邮箱发送验证码，用户键入验证码认证邮箱正确
        String encodedPassword = passwordEncoder.encode(userRegisterDTO.password());
        UserEntity userEntity = UserRegisterDTO.toEntity(userRegisterDTO, encodedPassword);
        try{
            userMapper.insertUser(userEntity);
        } catch (DuplicateKeyException e){
            throw new EmailOccupiedException(userRegisterDTO.email());
        }

    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserInfoDTO getUserById(String userId) throws UserNotFoundException, UserCanceledException {
        UserEntity userEntity = userMapper.selectUserByUserId(userId);
        if (userEntity == null){
            throw new UserNotFoundException(userId);
        }
        if (userEntity.status() == UserStatus.DELETED){
            throw new UserCanceledException(userId);
        }
        return userEntity.toUserInfoDTO();
    }
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public UserProfile getUserProfile(String userId) throws UserNotFoundException{
        UserEntity userEntity = userMapper.selectUserByUserId(userId);
        if (userEntity == null){
            throw new UserNotFoundException(userId);
        }
        return UserProfile.fromEntity(userEntity);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public List<UserInfoDTO> searchUserByName(String userName) {
        List<UserEntity> userEntities = userMapper.selectUserByName(userName);
        return userEntities.stream()
                .map(UserEntity::toUserInfoDTO)
                .toList();
    }
    // 用户注销, 返回被注销的用户id
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public Long cancelUser(String targetUserId, String operatorUserId) throws AuthorityException, UserNotFoundException {
        /**
         * 鉴定用户权限是否为admin
         */
        Long targetPKId = transformUserIdToPKId(targetUserId);
        Long operatorPKId = Long.parseLong(operatorUserId);
        if (!operatorPKId.equals(targetPKId)) {
            List<String> roles = userMapper.getUserRoles(operatorPKId);
            if (!roles.contains("ADMIN")) {
                throw new AuthorityException(operatorUserId);
            }
        }
        int updated = userMapper.updateUserStatus(targetPKId, UserStatus.DELETED);
        if (updated == 0) {
            throw new UserNotFoundException(targetUserId);
        }
        return targetPKId;
    }
    // 用户信息更新(昵称、头像、绑定邮箱、userId)；但是！邮箱应在迭代版本中独立为单独业务模块，此处暂不处理
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void updateUserInfo(UserInfoUpdate updateDto, String operatorUserId) throws UserException {
        try{
            long pkId = Long.parseLong(operatorUserId);
            if (pkId < 0) {
                throw new UserIdLostConnection(updateDto.userId());
            }
            int updated = userMapper.updateUserInfo(updateDto, pkId);
            if (updated == 0)
                throw new UserNotFoundException(Long.toString(pkId));
        } catch (DataAccessException | UserNotFoundException e){
            throw new UserException("USER_INFO_UPDATED_FAILED", e.getMessage());
        }
    }
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void updateUserStatus(Long targetUserPKId, UserStatus status) throws DuplicateKeyException, UserNotFoundException{
        int updated = userMapper.updateUserStatus(targetUserPKId, status);
        if (updated == 0){
            throw new UserNotFoundException(String.valueOf(targetUserPKId));
        }
    }
    public void registerPublicKey(Long targetUserPKId, String publicKey) throws DuplicateKeyException{
        userMapper.insertPublicKey(targetUserPKId, publicKey);
    }
//    暂时弃用，避免因修改导致的无法解密roomKey问题
//    public void updateUserPublicKey(Long targetUserPKId, String publicKey) throws UserNotFoundException {
//        int updated = userMapper.updatePublicKey(targetUserPKId, publicKey);
//        if (updated == 0){
//            log.warn("MySQL error while updating user public key");
//            throw new UserNotFoundException(String.valueOf(targetUserPKId));
//        } else if (updated > 0){
//            log.info("User public key updated successfully.");
//        } else {
//            log.error("Unexpected update result.");
//        }
//    }
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public String getUserPublicKey(Long targetUserId) throws UserNotFoundException {
        Long targetUserPKId = userIdentityResolver.getUserPKIdByUserId(String.valueOf(targetUserId));
        String publicKey = userMapper.getPublicKey(targetUserPKId);
        if (publicKey == null){
            throw new PublicKeyNotFoundException(String.valueOf(targetUserPKId));
        }
        return publicKey;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    protected Long transformUserIdToPKId(String userId) throws UserNotFoundException {
        Long pkId = userMapper.selectPKIdByUserId(userId);
        if (pkId == null){
            throw new UserNotFoundException(userId);
        }
        return pkId;
    }
}
