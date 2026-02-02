package com.chatroom.security.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class LoginService {
    private final AuthenticationManager authenticationManager;
    public LoginService(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    public void login(String userId, String password){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userId,
                        password
                )
        );

    }
}
