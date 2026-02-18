package com.chatroom.room.component;

import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.exception.RoomAuthorityException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class RoomAuthorization {
    private final RoomCacheRepository roomCacheRepository;
    public RoomAuthorization(RoomCacheRepository roomCacheRepository) {
        this.roomCacheRepository = roomCacheRepository;
    }
    public void checkRoomOwner(String roomId, String userPKId){
         if (!roomCacheRepository.isRoomOwner(roomId, userPKId)){
            throw new RoomAuthorityException(roomId);
         }
    }

    public void authorizeLeaveRoom(String roomId, String userPKId, String operatorPKId){
        // 房主踢人，允许
        if (roomCacheRepository.isRoomOwner(roomId, operatorPKId)){
            if (Objects.equals(userPKId, operatorPKId)){    // 房主自己退出
                Set<String> roomMembersId = roomCacheRepository.getRoomMembersId(roomId);    // 房主转让给其他成员
                roomMembersId.remove(operatorPKId);    // 把房主从集合移除
                roomCacheRepository.changeRoomOwner(roomId, roomMembersId.iterator().next());
            }
            return;
        }
        // 用户自身退出，允许
        if (userPKId.equals(operatorPKId)){
            return;
        }
        throw new RoomAuthorityException(roomId);
    }
}
