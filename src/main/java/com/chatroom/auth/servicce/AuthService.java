package com.chatroom.auth.servicce;

import com.chatroom.security.component.JwtProvider;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.security.exception.JwtGenerateException;
import com.nimbusds.jose.JOSEException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final JwtProvider jwtProvider;
    private final AuthenticationManager authenticationManager;
    public AuthService(JwtProvider jwtProvider, AuthenticationManager authenticationManager) {
        this.jwtProvider = jwtProvider;
        this.authenticationManager = authenticationManager;
    }
    public String login(String userId, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userId, password)
        );
        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
        try {
            return jwtProvider.generateJwt(securityUser);
        } catch (JOSEException e){
            throw new JwtGenerateException(userId);
        }

    }
}
