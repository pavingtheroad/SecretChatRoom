package com.chatroom.security.lifecycle;

import com.chatroom.component.UserIdentityResolver;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.user.dao.UserMapper;
import com.chatroom.user.entity.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityUserService implements UserDetailsService {
    private final UserMapper userMapper;
    private final UserIdentityResolver userIdentityResolver;

    public SecurityUserService(UserMapper userMapper, UserIdentityResolver userIdentityResolver) {
        this.userMapper = userMapper;
        this.userIdentityResolver = userIdentityResolver;
    }
    /**
     *
     * @param userId 这个userId是登录凭证，是可变userId
     * @return
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Long userPKId = userIdentityResolver.getUserPKIdByUserId(userId);
        UserEntity user = userMapper.selectUserByPKId(userPKId);
        List<GrantedAuthority> authorities = new ArrayList<>();
        userMapper.getUserRoles(userPKId).forEach(role -> {
            authorities.add(new SimpleGrantedAuthority(role));
        });
        return new SecurityUser(
                userPKId.toString(),
                user.password(),
                authorities
        );
    }
}
