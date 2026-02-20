package com.chatroom.room.controller;

import com.aventrix.jnanoid.jnanoid.NanoIdUtils;
import com.chatroom.room.dto.PutEncryptedKeyRequest;
import com.chatroom.util.ApiResponse;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.service.RoomOwnerService;
import com.chatroom.room.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/room")
public class RoomController {
    private final RoomService roomService;
    private final RoomOwnerService roomOwnerService;
    public RoomController(RoomService roomService, RoomOwnerService roomOwnerService) {
        this.roomService = roomService;
        this.roomOwnerService = roomOwnerService;
    }
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createRoom(@RequestBody RoomInfo roomInfo) {
        RoomInfo info = new RoomInfo(
                NanoIdUtils.randomNanoId(),
                roomInfo.roomName(),
                getUserIdFromToken(),
                roomInfo.description(),
                System.currentTimeMillis(),
                roomInfo.muted(),
                roomInfo.locked(),
                roomInfo.ttlMillis()
        );
        roomService.createRoom(info);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Room Created Successfully",
                null,
                null
        ));
    }
    @PostMapping("/{roomId}/members/me")
    public ResponseEntity<ApiResponse<String>> joinRoom(@PathVariable String roomId){
        String userId = getUserIdFromToken();    // 从请求的Token中获取
        roomService.joinRoom(roomId, userId);
        String publicKey = roomService.getEncryptedKey(roomId, userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Joined Room Successfully",
                publicKey,
                null
        ));
    }

    @GetMapping("/{roomId}/info")
    public ResponseEntity<ApiResponse<RoomInfo>> getRoomInfo(@PathVariable String roomId){
        RoomInfo roomInfo = roomService.getRoomInfo(roomId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Room Info Successfully",
                roomInfo,
                null
        ));
    }
    @GetMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<Set<String>>> getRoomMembersId(@PathVariable String roomId){
        Set<String> membersId = roomService.getRoomMembersId(roomId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Room Members Id Successfully",
                membersId,
                null
        ));
    }
    @GetMapping("/joined")
    public ResponseEntity<ApiResponse<Set<String>>> getJoinedRoomsId(){
        String userId = getUserIdFromToken();
        Set<String> roomsId = roomService.joinedRoomsId(userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Joined Rooms Id Successfully",
                roomsId,
                null
        ));
    }
    @DeleteMapping("/leave/{roomId}")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable String roomId){
        String userId = getUserIdFromToken();    // 从Token解析
        roomService.leaveRoom(roomId, userId, userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Leave Room Successfully",
                null,
                null
        ));
    }

//  房主功能接口

    /**
     * 房间locked时仅允许房主添加用户
     */
    @PostMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<Void>> addUserToRoom(@PathVariable String roomId, @RequestBody AddUserRequest request){
        String userId = request.userId();
        roomOwnerService.addUserToRoom(roomId, userId);    // Service中获取当前操作用户

        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Add User To Room Successfully",
                null,
                null
        ));
    }
    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> manageRoomInfo(@PathVariable String roomId, @RequestBody RoomInfoUpdate roomInfo){
        String operatorId = getUserIdFromToken();     // 留位
        roomOwnerService.manageRoomInfo(roomId, roomInfo);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Manage Room Info Successfully",
                null,
                null
        ));
    }
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String roomId){
        roomOwnerService.deleteRoom(roomId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Delete Room Successfully",
                null,
                null
        ));
    }
    @PutMapping("/{roomId}/encrypted-key")
    public ResponseEntity<ApiResponse<Void>> putEncryptedKey(@PathVariable String roomId, @RequestBody PutEncryptedKeyRequest request){
        String userId = request.userId();
        String encryptedKey = request.encryptedKey();
        roomOwnerService.putEncryptedKey(roomId, userId, encryptedKey);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Put Encrypted Key Successfully",
                null,
                null
        ));
    }
    private String getUserIdFromToken(){
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();
        return authentication.getName();
    }
}
