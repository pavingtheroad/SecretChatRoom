## 2025/12/11
### aim：房间管理设计
1. 确定 Redis 存储模型
   1. 房间元数据 `HASH` 
   
            `room:{roomId}
                  roomName
                  owner
                  description
                  createdAt
                  muted    (禁言)
                  locked    (是否公共开放)
                  ttlMillis`   
   2. 房间成员列表 `SET`

      `room:{roomId}:members `
   3. 用户加入的房间列表 `SET`
   
      `user:{userId}:rooms `
2. 接口设计（写数据时注意原子性）
   1. RoomRepository
      1. ~~createRoom `params: RoomInfoDTO`~~    创建房间
      2. ~~getRoomInfo `params: roomId` `return: RoomInfoDTO`~~    获取房间信息
      3. ~~addUserToRoom `params: roomId, userId`~~    用户加入房间
      4. ~~removeUserFromRoom `params: roomId, userId`~~    用户退出房间
      5. ~~getRoomMembers `params: roomId` `return: Set<String>`~~    房间成员表(获取id)
      6. ~~joinedRooms `params: userId` `return: Set<String>`~~    房间信息(获取id)
      7. deleteRoomAtomic `params: roomId`    删除房间（同时删除
                                          `room:{roomId} (hash) `
                                          `room:{roomId}:members`
                                          `chat:room:{roomId}:msg (redis stream)` 
                                          `chat:room:{roomId}:user:{userId}:cursor`
                                          `user:{userId}:rooms`
         因为要操作多个数据，所以要保证原子性：Lua / pipeline
      8. updateRoomInfo `params: roomId, RoomInfoDTO`    更新房间信息
      9. checkUserInRoom `params: roomId, userId` `return: boolean`
      10. getRoomsOfUser `params: userId` `return: List<String> (roomId:roomName)`    用户加入的房间列表
      11. roomExists `params: roomId` `return: boolean`    房间存在校验(读校验)
   2. RoomService
      1. createRoom `params: RoomInfoDTO`
      2. getRoomInfo `params: roomId` `return: RoomInfoDTO`    获取房间信息
      3. addUserToRoom `params: roomId, userId`    用户加入房间
      4. removeUserFromRoom `params: roomId, userId`    用户退出房间
      5. getRoomMembers `params: roomId` `return: List<UserInfoDTO>`    房间成员表(聚合具体信息)
      6. joinedRooms `params: userId` `return: RoomInfoDTO[]`    用户加入的房间列表
      7. deleteRoom `params: roomId`    删除房间
      8. updateRoomInfo `params: roomId, RoomInfoDTO`
      9. checkUserInRoom `params: roomId, userId` `return: boolean`
      10. roomExists `params: roomId` `return: boolean`    房间存在校验
      11. getRoomOfUser `params: userId` `return: List<String> (roomId:roomName)`    用户加入的房间列表
## 2025/12/16
### 完成房间管理模块
Notes
1. com.chatroom.room.repository：RoomCacheRepository中的addUserToRoom需要二次开发，缺少一致性与原子性维护