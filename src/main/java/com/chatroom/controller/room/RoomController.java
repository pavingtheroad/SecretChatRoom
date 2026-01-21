package com.chatroom.controller.room;

import com.chatroom.room.service.RoomServiceImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room")
public class RoomController {
    private final RoomServiceImpl roomServiceImpl;
    public RoomController(RoomServiceImpl roomServiceImpl) {
        this.roomServiceImpl = roomServiceImpl;
    }


}
