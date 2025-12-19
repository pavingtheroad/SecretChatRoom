package com.chatroom.room.service;

import com.chatroom.room.dto.RoomInfoDTO;
import com.chatroom.room.dto.UserInfoDTO;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.room.exception.UserAlreadyExistsException;
import com.chatroom.room.exception.UserNotFoundException;
import com.chatroom.room.repository.RoomCacheRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomService {
    private final RoomCacheRepository roomCacheRepository;
    public RoomService(RoomCacheRepository roomCacheRepository) {
        this.roomCacheRepository = roomCacheRepository;
    }

    public void createRoom(RoomInfoDTO roomInfo) throws RoomAlreadyExistsException {
        roomCacheRepository.createRoom(roomInfo);
    }

    public RoomInfoDTO getRoomInfo(String roomId) throws RoomNotFoundException {
        return roomCacheRepository.getRoomInfo(roomId);
    }

    public Boolean addUserToRoom(String roomId, String userId) throws RoomNotFoundException, UserAlreadyExistsException {
        return roomCacheRepository.addUserToRoom(roomId, userId);
    }

    public Boolean removeUserFromRoom(String roomId, String userId) throws RoomNotFoundException, UserNotFoundException {
        return roomCacheRepository.removeUserFromRoom(roomId, userId);
    }

    public List<UserInfoDTO> getRoomMembers(String roomId) throws RoomNotFoundException {
        Set<String> members = roomCacheRepository.getRoomMembers(roomId);
        // 获取用户info，通过用户repository功能 组合为List返回
        return null; // TODO: 实现完整的逻辑
    }

    public List<RoomInfoDTO> joinedRooms(String userId) throws RoomNotFoundException {
        List<RoomInfoDTO> roomInfoList = new ArrayList<>();
        Set<String> rooms = roomCacheRepository.joinedRooms(userId);
        List<Map<String, String>> roomInfoMap = roomCacheRepository.batchGetRoomInfo(rooms);
        roomInfoMap.stream()
                .forEach(
                        roomInfo -> {
                            roomInfoList.add(
                                    new RoomInfoDTO(
                                            roomInfo.get("roomId"),
                                            roomInfo.get("roomName"),
                                            roomInfo.get("owner"),
                                            roomInfo.get("description") == null ? "" : roomInfo.get("description"),
                                            Long.parseLong(roomInfo.get("createdAt")),
                                            Boolean.parseBoolean(roomInfo.get("muted")),
                                            Boolean.parseBoolean(roomInfo.get("locked")),
                                            Long.parseLong(roomInfo.get("ttlMillis"))
                                    )
                            );
                        }
                );
        return roomInfoList;
    }

    public Boolean deleteRoom(String roomId) throws RoomNotFoundException {
        return roomCacheRepository.deleteRoom(roomId);
    }

    public void updateRoomInfo(String roomId, RoomInfoDTO roomInfo) throws RoomNotFoundException {
        roomCacheRepository.updateRoomInfo(roomId, roomInfo);
    }

    public Boolean authorizeRoomAccess(String roomId, String userId) throws RoomNotFoundException {
        return roomCacheRepository.authorizeRoomAccess(roomId, userId);
    }
}
