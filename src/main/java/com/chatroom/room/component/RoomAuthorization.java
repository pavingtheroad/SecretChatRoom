package com.chatroom.room.component;

import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.exception.RoomAuthorityException;
import org.springframework.stereotype.Component;

@Component
public class RoomAuthorization {
    private final RoomCacheRepository roomCacheRepository;
    public RoomAuthorization(RoomCacheRepository roomCacheRepository) {
        this.roomCacheRepository = roomCacheRepository;
    }
    public void checkRoomOwner(String roomId, String userPKId){
         if (roomCacheRepository.isRoomOwner(roomId, userPKId)){
            throw new RoomAuthorityException(roomId);
         }
    }

    public void authorizeLeaveRoom(String roomId, String userPKId, String operatorPKId){
        // 用户自身退出，允许
        if (userPKId.equals(operatorPKId)){
            return;
        }
        // 房主踢人，允许
        if (roomCacheRepository.isRoomOwner(roomId, operatorPKId)){
            return;
        }
        throw new RoomAuthorityException(roomId);
    }
}
