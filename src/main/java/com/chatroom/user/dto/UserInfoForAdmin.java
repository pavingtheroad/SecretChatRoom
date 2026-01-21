package com.chatroom.user.dto;

import com.chatroom.user.domain.UserStatus;

import java.util.List;

public record UserInfoForAdmin(
    String userId,
    String userName,
    String avatarUrl,
    UserStatus status,
    List<String> roleCode
) {}
