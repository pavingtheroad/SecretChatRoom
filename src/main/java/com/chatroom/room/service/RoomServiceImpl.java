package com.chatroom.room.service;

import com.chatroom.message.dao.RoomStateRepository;
import com.chatroom.room.component.RoomAuthorization;
import com.chatroom.component.UserIdentityResolver;
import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.user.exception.UserNotFoundException;
import com.chatroom.room.dao.RoomCacheRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomService{
    private final RoomCacheRepository roomCacheRepository;
    private final UserIdentityResolver userIdentityResolver;
    private final RoomAuthorization roomAuthorization;
    private final RoomStateRepository roomStateRepository;
    public RoomServiceImpl(RoomCacheRepository roomCacheRepository,
                           UserIdentityResolver userIdentityResolver,
                           RoomAuthorization roomAuthorization,
                           RoomStateRepository roomStateRepository) {
        this.roomCacheRepository = roomCacheRepository;
        this.userIdentityResolver = userIdentityResolver;
        this.roomAuthorization = roomAuthorization;
        this.roomStateRepository = roomStateRepository;
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

        long result = roomCacheRepository.addUserToRoom(roomId, userPKId.toString());

        if (result == -2L){    // 房间处于locked状态
            throw new RoomAuthorityException(roomId);
        }
    }
    public Set<String> joinedRoomsId(String userId) {     // 获取用户加入的roomId集合
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
        roomCacheRepository.removeUserFromRoom(roomId, userPKId);
    }
    @Override
    public Boolean authorizeRoomAccess(String roomId, String userPKId) throws RoomNotFoundException{
        return roomCacheRepository.authorizeRoomAccess(roomId, userPKId);
    }
    @Override
    public Boolean roomExists(String roomId) {
        return roomCacheRepository.roomExists(roomId);
    }
}
