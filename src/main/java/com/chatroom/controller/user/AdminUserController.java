package com.chatroom.controller.user;

import com.chatroom.controller.apiresponse.ApiResponse;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.service.AdminUserService;
import com.chatroom.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminUserController {
    private final AdminUserService adminUserService;
    private final UserService userService;
    public AdminUserController(AdminUserService adminUserService, UserService userService) {
        this.adminUserService = adminUserService;
        this.userService = userService;
    }
    @PutMapping("/role")
    public ResponseEntity<ApiResponse<Void>> insertRoleToUser(String userId, String roleCode) {
        adminUserService.insertRoleToUser(userId, roleCode);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Adding Role" + roleCode + "To User" + userId + "Successfully",
                null,
                null));
    }
    @PutMapping("/banned")
    public ResponseEntity<ApiResponse<Void>> banUser(String userId){
        /**
         * Token获得操作者ID
         */
        String operatorUserId = "";
        userService.bannedUser(userId, operatorUserId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Banning User" + userId + "Successfully",
                null,
                null));
    }
    @GetMapping("/user-profile")
    public ResponseEntity<ApiResponse<UserInfoDTO>> getUserByUserId(String userId){
        UserInfoDTO userInfoDTO = userService.getUserById(userId);
        return ResponseEntity.ok(new ApiResponse<>("SUCCESS",
                "Getting User" + userId + "Profile Successfully",
                userInfoDTO,
                null));
    }

}
