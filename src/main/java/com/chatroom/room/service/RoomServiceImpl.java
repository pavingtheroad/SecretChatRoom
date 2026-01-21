package com.chatroom.room.service;

import com.chatroom.component.RoomAuthorization;
import com.chatroom.component.UserIdentityResolver;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.user.dto.UserInfoDTO;
import com.chatroom.user.exception.AuthorityException;
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
public class RoomServiceImpl implements RoomService{
    private final RoomCacheRepository roomCacheRepository;
    private final UserIdentityResolver userIdentityResolver;
    private final RoomAuthorization roomAuthorization;
    public RoomServiceImpl(RoomCacheRepository roomCacheRepository, UserIdentityResolver userIdentityResolver, RoomAuthorization roomAuthorization) {
        this.roomCacheRepository = roomCacheRepository;
        this.userIdentityResolver = userIdentityResolver;
        this.roomAuthorization = roomAuthorization;
    }

    public void createRoom(RoomInfo roomInfo) throws RoomAlreadyExistsException {
        roomCacheRepository.createRoom(roomInfo);
    }

    public RoomInfo getRoomInfo(String roomId) throws RoomNotFoundException {
        return roomCacheRepository.getRoomInfo(roomId);
    }
    @Override
    public void joinRoom(String roomId, String userId) throws UserNotFoundException{
        Long userPKId = userIdentityResolver.getUserPKIdByUserId(userId);
        roomCacheRepository.addUserToRoom(roomId, userPKId.toString());
    }
    public Set<String> joinedRoomsId(String userId) {     // 获取用户加入的roomId集合
        List<String> roomIds = new ArrayList<>();
        return roomCacheRepository.joinedRooms(userIdentityResolver.getUserPKIdByUserId(userId).toString());
    }
    @Override
    public Set<String> getRoomMembersId(String roomId) {
        return roomCacheRepository.getRoomMembersId(roomId);
    }
    @Override
    public void leaveRoom(String roomId, String userId, String operatorId){
        String userPKId = userIdentityResolver.getUserPKIdByUserId(userId).toString();
        String operatorPKId = userIdentityResolver.getUserPKIdByUserId(operatorId).toString();
        roomAuthorization.authorizeLeaveRoom(roomId, userPKId, operatorPKId);
        roomCacheRepository.removeUserFromRoom(roomId, userIdentityResolver.getUserPKIdByUserId(userId).toString());
    }
//    public List<RoomInfo> joinedRooms(String userPKId) throws RoomNotFoundException {
//        List<RoomInfo> roomInfoList = new ArrayList<>();
//        Set<String> rooms = roomCacheRepository.joinedRooms(userPKId);
//        List<Map<String, String>> roomInfoMap = roomCacheRepository.batchGetRoomInfo(rooms);
//        roomInfoMap
//                .forEach(
//                        roomInfo -> {
//                            roomInfoList.add(
//                                    new RoomInfo(
//                                            roomInfo.get("roomId"),
//                                            roomInfo.get("roomName"),
//                                            roomInfo.get("owner"),
//                                            roomInfo.get("description") == null ? "" : roomInfo.get("description"),
//                                            Long.parseLong(roomInfo.get("createdAt")),
//                                            Boolean.parseBoolean(roomInfo.get("muted")),
//                                            Boolean.parseBoolean(roomInfo.get("locked")),
//                                            Long.parseLong(roomInfo.get("ttlMillis"))
//                                    )
//                            );
//                        }
//                );
//        return roomInfoList;
//    }

    public Boolean authorizeRoomAccess(String roomId, String userPKId) throws RoomNotFoundException {
        return roomCacheRepository.authorizeRoomAccess(roomId, userPKId);
    }
}
