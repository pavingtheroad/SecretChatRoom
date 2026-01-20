package com.chatroom.controller.user;

import com.chatroom.facade.UserLifecycleService;
import com.chatroom.user.dto.UserInfoUpdate;
import com.chatroom.user.dto.UserProfile;
import com.chatroom.user.dto.UserRegisterDTO;
import com.chatroom.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<String> updateUserInfo(@RequestBody UserInfoUpdate userInfoUpdate){
        US.updateUserInfo(userInfoUpdate);
        return ResponseEntity.ok().build();

    }
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<String> deleteUser(@PathVariable String targetUserId){
            String operatorUserId = "admin";    // operateUserId 取自JWT 此处仅为测试，实际操作中应该从JWT中获取
            ULS.cancelUser(targetUserId, operatorUserId);
            return ResponseEntity.ok().build();

    }
    @GetMapping("/me")
    public ResponseEntity<String> getUserProfile(@RequestParam String userId){
        UserProfile userProfile = US.getUserProfile(userId);
        return ResponseEntity.ok(userProfile.toString());
    }
}
