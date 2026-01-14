package com.chatroom.user.dao;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    int insertUser(UserEntity user);

    UserEntity selectByUserId(@Param("userId") String userId);

    List<UserEntity> selectByUserName(@Param("userName") String userName);

    int updateUserStatus(@Param("userId") String userId, @Param("status") UserStatus status);

    int updateUserInfo(UserEntity user);

    List<String> getUserRoles(@Param("userId") String userId);
}
