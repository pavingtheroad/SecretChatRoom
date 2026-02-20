package com.chatroom.room.service;

import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.security.component.IdentityResolver;
import com.chatroom.room.component.RoomAuthorization;
import com.chatroom.component.UserIdentityResolver;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RoomOwnerServiceImpl implements RoomOwnerService{
    private final RoomCacheRepository roomCacheRepository;
    private final UserIdentityResolver userIdentityResolver;
    private final RoomAuthorization roomAuthorization;
    public RoomOwnerServiceImpl(RoomCacheRepository roomCacheRepository,
                                UserIdentityResolver userIdentityResolver,
                                RoomAuthorization roomAuthorization) {
        this.roomCacheRepository = roomCacheRepository;
        this.userIdentityResolver = userIdentityResolver;
        this.roomAuthorization = roomAuthorization;
    }
    @Override
    public void addUserToRoom(String roomId, String userId) {
        String operatorPKId = IdentityResolver.currentUserPKId().toString();
        roomAuthorization.checkRoomOwner(roomId, operatorPKId);
        String userPKId = userIdentityResolver.getUserPKIdByUserId(userId).toString();
        roomCacheRepository.addUserToRoom(roomId, userPKId);
    }

    @Override
    public void manageRoomInfo(String roomId, RoomInfoUpdate roomInfo) {
        String userPKId = IdentityResolver.currentUserPKId().toString();
        roomAuthorization.checkRoomOwner(roomId, userPKId);
        roomCacheRepository.updateRoomInfo(roomId, roomInfo);
    }

    @Override
    public void deleteRoom(String roomId) throws RoomNotFoundException {
        String userPKId = IdentityResolver.currentUserPKId().toString();
        roomAuthorization.checkRoomOwner(roomId, userPKId);
        roomCacheRepository.deleteRoom(roomId);

    }
    /**
     * 房间邀请用户加入时就调用，用于分配房间密钥
     */
    @Override
    public void putEncryptedKey(String roomId, String userId, String encryptedKey) throws UserNotFoundException {
        String userPKId = String.valueOf(userIdentityResolver.getUserPKIdByUserId(userId));
        roomCacheRepository.putEncryptedRoomKey(roomId, userPKId, encryptedKey);
    }
}