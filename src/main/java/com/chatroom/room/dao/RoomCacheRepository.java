package com.chatroom.room.dao;

import com.chatroom.room.dto.RoomInfo;
import com.chatroom.room.dto.RoomInfoUpdate;
import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.user.exception.UserAlreadyExistsException;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class RoomCacheRepository {
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> deleteRoomScript;
    private final RedisScript<Long> addUserToRoomScript;
    private final RedisScript<Long> removeUserFromRoomScript;
    private final RedisScript<Long> createRoomScript;
    private final RedisScript<List> batchGetRoomInfoScript;
    public RoomCacheRepository(
            StringRedisTemplate redisTemplate,
            RedisScript<Long> deleteRoomScript,
            RedisScript<Long> addUserToRoomScript,
            RedisScript<Long> removeUserFromRoomScript,
            RedisScript<Long> createRoomScript,
            RedisScript<List> batchGetRoomInfoScript) {
        this.redisTemplate = redisTemplate;
        this.deleteRoomScript = deleteRoomScript;
        this.addUserToRoomScript = addUserToRoomScript;
        this.removeUserFromRoomScript = removeUserFromRoomScript;
        this.createRoomScript = createRoomScript;
        this.batchGetRoomInfoScript = batchGetRoomInfoScript;
    }

    private static final String ROOM_KEY_PREFIX = "chat:room:";
    public void createRoom(RoomInfo roomInfo) throws RoomAlreadyExistsException {
        String roomId = roomInfo.roomId();
        String key = ROOM_KEY_PREFIX + roomId;
        List<String> keys = List.of(key);
        Long result = redisTemplate.execute(
                createRoomScript,
                keys,
                roomInfo.roomName(),
                roomInfo.ownerId(),
                roomInfo.description() == null ? "" : roomInfo.description(),
                roomInfo.createdAt(),
                roomInfo.muted(),
                roomInfo.locked(),
                roomInfo.ttlMillis(),
                roomId
        );
        if (result == 0L) throw new RoomAlreadyExistsException(roomId);
    }

    public RoomInfo getRoomInfo(String roomId) throws RoomNotFoundException {
        String key = ROOM_KEY_PREFIX + roomId;
        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(key);
        if (roomData.isEmpty()){
            throw new RoomNotFoundException(roomId);
        }
        return new RoomInfo(
                roomId,
                (String) roomData.get("roomName"),
                (String) roomData.get("ownerId"),
                (String) roomData.get("description"),
                roomData.get("createdAt") != null ? Long.parseLong((String) roomData.get("createdAt")) : null,
                roomData.get("muted") != null ? Boolean.parseBoolean((String) roomData.get("muted")) : null,
                roomData.get("locked") != null ? Boolean.parseBoolean((String) roomData.get("locked")) : null,
                roomData.get("ttlMillis") != null ? Long.parseLong((String) roomData.get("ttlMillis")) : null
        );
    }
    // 批量获取房间信息
    public List<Map<String, String>> batchGetRoomInfo(Set<String> roomIds){
        List<String> keys = roomIds.stream()
                .map(roomId -> ROOM_KEY_PREFIX + roomId)
                .toList();

        List<Object> roomDatas = redisTemplate.execute(batchGetRoomInfoScript, keys);

        List<Map<String, String>> result = new ArrayList<>();

        for (Object roomData : roomDatas) {
            List<String> flat = (List<String>) roomData;
            Map<String, String> roomDataMap = new HashMap<>();
            for (int i = 0; i < flat.size(); i += 2) {
                roomDataMap.put(flat.get(i), flat.get(i + 1));
            }
            result.add(roomDataMap);
        }
        return result;
    }
    // 默认在此前进行了用户存在性校验
    public long addUserToRoom(String roomId, String userPKId) throws RoomNotFoundException, UserAlreadyExistsException {        // 参数userPKId对应数据库中id
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String roomUserKey = roomKey + ":members";
        String userKey = "user:" + userPKId + ":rooms";

        List<String> keys = List.of(roomKey, roomUserKey, userKey);

        long result = redisTemplate.execute(addUserToRoomScript, keys, userPKId, roomId);
        if (result == 0L) throw new RoomNotFoundException(roomId);
        else if (result == -1L) throw new UserAlreadyExistsException(userPKId);
        return result;    // 只过滤掉房间不存在和用户已添加的情况
    }

    public Boolean removeUserFromRoom(String roomId, String userPKId) throws RoomNotFoundException, UserNotFoundException {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String membersKey = ROOM_KEY_PREFIX + roomId + ":members";
        String userKey = "user:" + userPKId + ":rooms";
        List<String> keys = List.of(roomKey, membersKey, userKey);

        Long result = redisTemplate.execute(removeUserFromRoomScript, keys, userPKId, roomId);

        if (result == 0L) throw new RoomNotFoundException(roomId);
        else if (result == -1L) throw new UserNotFoundException(userPKId);
        else return true;
    }

    public Set<String> getRoomMembersId(String roomId) throws RoomNotFoundException {
        if(!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        Set<String> members = redisTemplate.opsForSet().members(ROOM_KEY_PREFIX + roomId + ":members");
        return members == null ? Collections.emptySet() : members;
    }

    public Set<String> joinedRooms(String userPKId){
        Set<String> rooms = redisTemplate.opsForSet().members("user:" + userPKId + ":rooms");    // 进一步优化时可以做分页
        return rooms == null ? Collections.emptySet() : rooms;
    }

    public boolean deleteRoom(String roomId) throws RoomNotFoundException {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String membersKey = roomKey + ":members";

        List<String> keys = List.of(roomKey, membersKey);

        Long result = redisTemplate.execute(deleteRoomScript, keys, roomId);

        if (result == null) {
            throw new IllegalStateException("deleteRoom script returned null");
        }
        if (result == 0L){
            throw new RoomNotFoundException(roomId);
        }
        return result == 1L;
    }

    public void updateRoomInfo(String roomId, RoomInfoUpdate roomInfo) throws RoomNotFoundException {
        String key = ROOM_KEY_PREFIX + roomId;
        if (!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        Map<String, Object> update = new HashMap<>();
        if (roomInfo.roomName() != null) update.put("roomName", roomInfo.roomName());
        if (roomInfo.description() != null) update.put("description", roomInfo.description());
        if (roomInfo.muted() != null) update.put("muted", roomInfo.muted());
        if (roomInfo.locked() != null) update.put("locked", roomInfo.locked());
        if (roomInfo.ttlMillis() != null) update.put("ttlMillis", roomInfo.ttlMillis());

        if (!update.isEmpty()){
            redisTemplate.opsForHash().putAll(key, update);
        }

    }
    // 用户是否属于房间
    public Boolean authorizeRoomAccess(String roomId, String userPKId){
        String key = ROOM_KEY_PREFIX + roomId;
        if (!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        return redisTemplate.opsForSet().isMember(key + ":members", userPKId);
    }
    /**
     * 用户是否为房主
     */
    public Boolean isRoomOwner(String roomId, String userPKId){
        if (!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        return Objects.equals(redisTemplate.opsForHash().get(ROOM_KEY_PREFIX + roomId, "ownerId"), userPKId);
    }

    // 仅用于读路径的快速校验
    public boolean roomExists(String roomId){
        return redisTemplate.hasKey(ROOM_KEY_PREFIX + roomId);
    }

    public Set<String> getAllRoomIds(){
        return redisTemplate.opsForSet().members("chat:room");
    }
}
