package com.chatroom.room.service;

import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.exception.RoomNotFoundException;

import java.util.Set;

public interface RoomService {
    void createRoom(RoomInfo roomInfo);

    RoomInfo getRoomInfo(String roomId);

    void joinRoom(String roomId, String userId);

    void leaveRoom(String roomId, String userId, String operatorId);

//    List<RoomInfo> getJoinedRoomInfos(String userPKId);     // 好像不用写？批量获取只是需要获取List<Id>再循环getRoomInfo
    Set<String> joinedRoomsId(String userPKId);

    Set<String> getRoomMembersId(String roomId);    // 或许一次请求把信息全部放在前端？或者只返回ids，进一步查看再返回单个用户信息？

    Boolean authorizeRoomAccess(String roomId, String userPKId) throws RoomNotFoundException;

    Boolean roomExists(String roomId);    // 仅用于读路径的快速校验

    String getEncryptedKey(String roomId, String userPKId);
}
