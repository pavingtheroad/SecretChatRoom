package com.chatroom.room.dao;

import com.chatroom.room.dto.RoomInfoDTO;
import com.chatroom.room.exception.RoomAlreadyExistsException;
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

    private static final String ROOM_KEY_PREFIX = "room:";
    public void createRoom(RoomInfoDTO roomInfo) throws RoomAlreadyExistsException {
        String key = ROOM_KEY_PREFIX + roomInfo.roomId();
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
                roomInfo.ttlMillis()
        );

        if (result == 0L) throw new RoomAlreadyExistsException(roomInfo.roomId());

    }

    public RoomInfoDTO getRoomInfo(String roomId) throws RoomNotFoundException {
        String key = ROOM_KEY_PREFIX + roomId;
        if(!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        Map<Object, Object> roomData = redisTemplate.opsForHash().entries(key);
        return new RoomInfoDTO(
                roomId,
                (String) roomData.get("roomName"),
                (String) roomData.get("ownerId"),
                (String) roomData.get("description"),
                (Long) roomData.get("createdAt"),
                (Boolean) roomData.get("muted"),
                (Boolean) roomData.get("locked"),
                (Long) roomData.get("ttlMillis")
        );
    }

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

    public Boolean addUserToRoom(String roomId, String userPKId) throws RoomNotFoundException, UserAlreadyExistsException {        // 参数userPKId对应数据库中id
        /**
         * 用户存在性校验
         * throw new UserNotFoundException(userId);
         */
        // 还需要补充下列逻辑的一致性问题，通过事务或 Lua 脚本解决
        String roomKey = ROOM_KEY_PREFIX + roomId + ":members";
        String userKey = "user:" + userPKId + ":rooms";

        List<String> keys = List.of(roomKey, userKey);

        Long result = redisTemplate.execute(addUserToRoomScript, keys, roomId, userPKId);
        if (result == 0L) throw new RoomNotFoundException(roomId);
        else if (result == -1L) throw new UserAlreadyExistsException(userPKId);
        else return true;
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

    public Set<String> getRoomMembers(String roomId) throws RoomNotFoundException {
        if(!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        Set<String> members = redisTemplate.opsForSet().members(ROOM_KEY_PREFIX + roomId + ":members");
        return members == null ? Collections.emptySet() : members;
    }

    public Set<String> joinedRooms(String userPKId){
        /**
         * 用户存在性校验
         */
        Set<String> rooms = redisTemplate.opsForSet().members("user:" + userPKId + ":rooms");    // 进一步优化时可以做分页
        return rooms == null ? Collections.emptySet() : rooms;
    }

    public boolean deleteRoom(String roomId) throws RoomNotFoundException {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        String membersKey = roomKey + ":members";

        List<String> keys = List.of(roomKey, membersKey);

        Long result = redisTemplate.execute(deleteRoomScript, keys, roomId);

        if (result == 0L){
            throw new RoomNotFoundException(roomId);
        }
        if (result == null) {
            throw new IllegalStateException("deleteRoom script returned null");
        }
        return result != null && result == 1L;
    }

    public void updateRoomInfo(String roomId, RoomInfoDTO roomInfo) throws RoomNotFoundException {
        String key = ROOM_KEY_PREFIX + roomId;
        if (!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        Map<String, Object> update = new HashMap<>();
        if (roomInfo.roomName() != null) update.put("roomName", roomInfo.roomName());
        if (roomInfo.ownerId() != null) update.put("ownerId", roomInfo.ownerId());
        if (roomInfo.description() != null) update.put("description", roomInfo.description());
        if (roomInfo.muted() != null) update.put("muted", roomInfo.muted());
        if (roomInfo.locked() != null) update.put("locked", roomInfo.locked());
        if (roomInfo.ttlMillis() != null) update.put("ttlMillis", roomInfo.ttlMillis());

        if (!update.isEmpty()){
            redisTemplate.opsForHash().putAll(key, update);
        }

    }

    public Boolean authorizeRoomAccess(String roomId, String userPKId) throws RoomNotFoundException {
        String key = ROOM_KEY_PREFIX + roomId;
        if (!roomExists(roomId)){
            throw new RoomNotFoundException(roomId);
        }
        return redisTemplate.opsForSet().isMember(key + ":members", userPKId);
    }

    // 仅用于读路径的快速校验
    public boolean roomExists(String roomId){
        return redisTemplate.hasKey(ROOM_KEY_PREFIX + roomId);
    }
}
