package com.chatroom.component;

import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserIdentityResolver {
    private final UserMapper userMapper;
    public UserIdentityResolver(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    public Long getUserPKIdByUserId(String userId) throws UserNotFoundException{
        Long pkId = userMapper.selectPKIdByUserId(userId);
        if (pkId == null || pkId <= 0){
            throw new UserNotFoundException(userId);
        }
        return pkId;
    }
}
