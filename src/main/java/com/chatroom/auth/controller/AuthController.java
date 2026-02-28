package com.chatroom.auth.controller;

import com.chatroom.auth.dto.LoginRequest;
import com.chatroom.auth.dto.LoginResponse;
import com.chatroom.auth.servicce.AuthService;
import com.chatroom.user.dto.UserRegisterDTO;
import com.chatroom.user.service.UserService;
import com.chatroom.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserService US;
    public AuthController(AuthService authService, UserService US) {
        this.authService = authService;
        this.US = US;
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
    // 注册账户
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserRegisterDTO userRegisterDTO){
        US.registerUser(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }
}
