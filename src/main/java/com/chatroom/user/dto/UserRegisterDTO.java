package com.chatroom.user.dto;

import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.entity.UserEntity;

public record UserRegisterDTO(
        String userName,
        String password,
        String email,
        String avatarUrl
) {
   public static UserEntity toEntity(UserRegisterDTO userRegisterDTO, String userId, String encodedPassword){
       return new UserEntity(
           null,
           userId,
           userRegisterDTO.userName(),
           userRegisterDTO.avatarUrl(),
           encodedPassword,
           userRegisterDTO.email(),
           UserStatus.ACTIVE
       );
   }

}
