package com.chatroom.controller.user;

import com.chatroom.controller.apiresponse.ApiResponse;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.dto.UserInfoForAdmin;
import com.chatroom.user.service.AdminUserService;
import com.chatroom.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminUserController {
    private final AdminUserService adminUserService;
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }
    @PutMapping("/role")
    public ResponseEntity<ApiResponse<Void>> insertRoleToUser(String userId, String roleCode) {
        adminUserService.insertRoleToUser(userId, roleCode);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Adding Role" + roleCode + "To User" + userId + "Successfully",
                null,
                null));
    }
    @PutMapping("/banned/{userId}")
    public ResponseEntity<ApiResponse<Void>> banUser(@PathVariable String userId){
        /**
         * Token获得操作者ID
         */
        String operatorUserId = SecurityContextUtil.getCurrentUserId();
        adminUserService.banUser(userId, operatorUserId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Banning User" + userId + "Successfully",
                null,
                null));
    }
    @GetMapping("/user-profile")
    public ResponseEntity<ApiResponse<UserInfoForAdmin>> getUserByUserId(String userId){
        UserInfoForAdmin userInfo = adminUserService.getUserInfo(userId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Getting User" + userId + "Profile Successfully",
                userInfo,
                null));
    }

}
