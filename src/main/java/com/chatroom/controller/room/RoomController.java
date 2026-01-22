package com.chatroom.controller.room;

import com.chatroom.controller.apiresponse.ApiResponse;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.service.RoomOwnerService;
import com.chatroom.room.service.RoomService;
import com.chatroom.room.service.RoomServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        roomService.createRoom(roomInfo);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Room Created Successfully",
                null,
                null
        ));
    }
    @PostMapping("/{roomId}/members/me")
    public ResponseEntity<ApiResponse<Void>> joinRoom(@PathVariable String roomId){
        String userId = "";    // 从请求的Token中获取，这里留位
        roomService.joinRoom(roomId, userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Joined Room Successfully",
                null,
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
        String userId = "";     // 留位，从token解析
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
        String userId = "";    // 从Token解析
        roomService.leaveRoom(roomId, userId, userId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Leave Room Successfully",
                null,
                null
        ));
    }

//  房主功能接口

    @PostMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<Void>> addUserToRoom(@PathVariable String roomId, @RequestBody AddUserRequest request){
        String operatorId = "";     // 留位
        String userId = request.userId();
        roomOwnerService.addUserToRoom(roomId, userId, operatorId);

        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Add User To Room Successfully",
                null,
                null
        ));
    }
    @PutMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> manageRoomInfo(@PathVariable String roomId, @RequestBody RoomInfoUpdate roomInfo){
        String operatorId = "";     // 留位
        roomOwnerService.manageRoomInfo(roomId, roomInfo, operatorId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Manage Room Info Successfully",
                null,
                null
        ));
    }
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable String roomId){
        String operatorId = "";
        roomOwnerService.deleteRoom(roomId, operatorId);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Delete Room Successfully",
                null,
                null
        ));
    }
}
