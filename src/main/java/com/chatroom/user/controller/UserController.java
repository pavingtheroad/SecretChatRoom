package com.chatroom.user.controller;

import com.chatroom.facade.UserLifecycleService;
import com.chatroom.user.dto.UserInfoUpdate;
import com.chatroom.user.dto.UserProfile;
import com.chatroom.user.dto.UserRegisterDTO;
import com.chatroom.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService US;
    private final UserLifecycleService ULS;
    public UserController (UserService userService, UserLifecycleService userLifecycleService){
        this.US = userService;
        this.ULS = userLifecycleService;
    }
    // 注册账户
    @PostMapping
    public ResponseEntity<String> registerUser(@RequestBody UserRegisterDTO userRegisterDTO){
        US.registerUser(userRegisterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }
    // 用户信息更新
    @PutMapping
    public ResponseEntity<String> updateUserInfo(@RequestBody UserInfoUpdate userInfoUpdate,
                                                 Authentication authentication){
        US.updateUserInfo(userInfoUpdate, authentication.getName());    // authentication的name一定不为空吗？
        return ResponseEntity.ok().build();

    }
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<String> deleteUser(@PathVariable String targetUserId,
                                             Authentication authentication){
            String operatorUserId = authentication.getName();    // operateUserId 取自JWT
            ULS.cancelUser(targetUserId, operatorUserId);
            return ResponseEntity.ok().build();

    }
    @GetMapping("/me")
    public ResponseEntity<String> getUserProfile(Authentication authentication){
        UserProfile userProfile = US.getUserProfile(authentication.getName());
        return ResponseEntity.ok(userProfile.toString());
    }
}
