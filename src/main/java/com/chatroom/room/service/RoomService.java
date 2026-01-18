package com.chatroom.room.service;

import com.chatroom.room.dto.RoomInfoDTO;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.exception.UserAlreadyExistsException;
import com.chatroom.user.exception.UserCanceledException;
import com.chatroom.user.exception.UserNotFoundException;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RoomService {
    private final RoomCacheRepository roomCacheRepository;
    private final UserService userService;
    public RoomService(RoomCacheRepository roomCacheRepository, UserService userService) {
        this.roomCacheRepository = roomCacheRepository;
        this.userService = userService;
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
        Set<String> membersId = roomCacheRepository.getRoomMembers(roomId);
        List<UserInfoDTO> members = new ArrayList<>();
        // 获取用户info，通过用户repository功能 组合为List返回
        membersId.forEach(
                memberId -> {
                    try {
                        members.add(userService.getUserById(memberId));
                    } catch (UserNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (UserCanceledException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return members;
    }

    public List<RoomInfoDTO> joinedRooms(String userPKId) throws RoomNotFoundException {
        List<RoomInfoDTO> roomInfoList = new ArrayList<>();
        Set<String> rooms = roomCacheRepository.joinedRooms(userPKId);
        List<Map<String, String>> roomInfoMap = roomCacheRepository.batchGetRoomInfo(rooms);
        roomInfoMap
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
    public Set<String> joinedRoomsId(String userPKId) {     // 获取用户加入的roomId集合
        List<String> roomIds = new ArrayList<>();
        return roomCacheRepository.joinedRooms(userPKId);
    }

    public Boolean deleteRoom(String roomId) throws RoomNotFoundException {
        return roomCacheRepository.deleteRoom(roomId);
    }

    public void updateRoomInfo(String roomId, RoomInfoDTO roomInfo) throws RoomNotFoundException {
        roomCacheRepository.updateRoomInfo(roomId, roomInfo);
    }

    public Boolean authorizeRoomAccess(String roomId, String userPKId) throws RoomNotFoundException {
        return roomCacheRepository.authorizeRoomAccess(roomId, userPKId);
    }
}
