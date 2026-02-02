package com.chatroom.auth.controller;

import com.chatroom.auth.dto.LoginRequest;
import com.chatroom.auth.dto.LoginResponse;
import com.chatroom.auth.servicce.AuthService;
import com.chatroom.util.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = new LoginResponse(authService.login(loginRequest.userId(), loginRequest.password()));
        return new ApiResponse<>(
                "SUCCESS",
                "Login Successfully",
                loginResponse,
                null
        );
    }
}
