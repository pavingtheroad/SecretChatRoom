package com.chatroom.message.controller;

import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.service.MessageQueryService;
import com.chatroom.room.service.RoomService;
import com.chatroom.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rooms")
public class MessageQueryController {
    private final MessageQueryService messageQueryService;
    private final RoomService roomService;
    public MessageQueryController(MessageQueryService messageQueryService,
                                  RoomService roomService) {
        this.messageQueryService = messageQueryService;
        this.roomService = roomService;
    }
    @GetMapping("/{roomId}/messages/init")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> initMessageQuery(@PathVariable String roomId,
                                                                          @RequestParam int limit,
                                                                          Authentication authentication){
        String userPkId = authentication.getName();
        if (!roomService.authorizeRoomAccess(roomId, userPkId)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(
                            "UNAUTHORIZED",
                            "User is not authorized to access this room",
                            null,
                            null
                    ));
        }
        limit = Math.min(limit, 100);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Messages Successfully",
                messageQueryService.initMessageQuery(roomId, userPkId, limit),
                null
        ));
    }
    @GetMapping("/{roomId}/messages/earlier")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getEarlierMessages(@PathVariable String roomId,
                                                                            @RequestParam String start,
                                                                            @RequestParam(defaultValue = "10") int limit,
                                                                            Authentication authentication){
        validateStreamId(start);
        String userPkId = authentication.getName();
        if (!roomService.authorizeRoomAccess(roomId, userPkId)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(
                            "UNAUTHORIZED",
                            "User is not authorized to access this room",
                            null,
                            null
                    ));
        }
        limit = Math.min(limit, 100);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Messages Successfully",
                messageQueryService.getForwardMessages(roomId, userPkId, start, limit),
                null
        ));
    }
    @GetMapping("/{roomId}/messages/later")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getLaterMessages(@PathVariable String roomId,
                                                                          @RequestParam String end,
                                                                          @RequestParam(defaultValue = "10") int limit,
                                                                          Authentication authentication){
        validateStreamId(end);
        String userPkId = authentication.getName();
        if (!roomService.authorizeRoomAccess(roomId, userPkId)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(
                            "UNAUTHORIZED",
                            "User is not authorized to access this room",
                            null,
                            null
                    ));
        }
        limit = Math.min(limit, 100);
        return ResponseEntity.ok(new ApiResponse<>(
                "SUCCESS",
                "Get Messages Successfully",
                messageQueryService.getBackwardMessages(roomId, userPkId, end, limit),
                null
        ));
    }
    private void validateStreamId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Stream ID cannot be empty");
        }

        String[] parts = id.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Stream ID format");
        }

        try {
            long timestamp = Long.parseLong(parts[0]);
            long sequence = Long.parseLong(parts[1]);
            if (timestamp < 0 || sequence < 0) {
                throw new IllegalArgumentException("Stream ID must be positive numbers");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Stream ID must be numeric");
        }
    }
}
