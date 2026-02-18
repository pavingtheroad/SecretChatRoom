package com.chatroom.user.controller;

import com.chatroom.util.ApiResponse;
import com.chatroom.user.dto.UserInfoForAdmin;
import com.chatroom.user.service.AdminUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminUserController {
    private final AdminUserService adminUserService;
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }
    @PutMapping("/role")
    public ResponseEntity<ApiResponse<Void>> insertRoleToUser(@RequestParam String userId,
                                                              @RequestParam String roleCode) {
        adminUserService.insertRoleToUser(userId, roleCode);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Adding Role" + roleCode + "To User" + userId + "Successfully",
                null,
                null));
    }
    @PutMapping("/banned/{userId}")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable String userId,
                                                     Authentication authentication){
        String operatorUserId = authentication.getName();
        adminUserService.banUser(userId, operatorUserId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Banning User" + userId + "Successfully",
                null,
                null));
    }
    @GetMapping("/user-profile/{userId}")
    public ResponseEntity<ApiResponse<UserInfoForAdmin>> getUserByUserId(@PathVariable String userId){
        UserInfoForAdmin userInfo = adminUserService.getUserInfo(userId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Getting User" + userId + "Profile Successfully",
                userInfo,
                null));
    }
}
