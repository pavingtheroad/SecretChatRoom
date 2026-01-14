package com.chatroom.user.dto;

import com.chatroom.user.entity.UserEntity;

public record UserRegisterDTO(
        String userName,
        String password,
        String email,
        String avatarUrl
) {
   public static UserEntity toEntity(UserRegisterDTO userRegisterDTO, String encodedPassword){
       return new UserEntity(
           null,
           null,
           userRegisterDTO.userName(),
           userRegisterDTO.avatarUrl(),
           encodedPassword,
           userRegisterDTO.email(),
           null
       );
   }

}
