package com.chatroom.user.dao;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoUpdate;
import com.chatroom.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    int insertUser(UserEntity user);

    Long selectPKIdByUserId(@Param("userId") String userId);    // 内部业务主查询方法

    UserEntity selectUserByPKId(@Param("id") Long id);      // 双ID桥梁

    UserEntity selectUserByUserId(@Param("userId") String userId);

    List<UserEntity> selectUserByName(@Param("userName") String userName);

    int updateUserStatus(@Param("id") Long id, @Param("status") UserStatus status);

    int updateUserInfo(UserInfoUpdate userInfo, @Param("id") Long id);

    List<String> getUserRoles(@Param("id") Long id);

    Long getRoleIdByCode(@Param("role_code") String code);

    int insertUserRoleTable(@Param("user_id") Long userPKId, @Param("role_id") Long roleId);
}
