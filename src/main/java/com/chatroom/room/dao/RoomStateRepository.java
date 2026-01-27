package com.chatroom.room.dao;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RoomStateRepository {
    private static final String ROOM_STATE_KEY_PREFIX = "chat:room:";
    private final StringRedisTemplate SRT;
    public RoomStateRepository(StringRedisTemplate srt) {
        SRT = srt;
    }
    public void createRoomState(String roomId){
        String key = ROOM_STATE_KEY_PREFIX + roomId + ":state";
        SRT.opsForHash().putIfAbsent(key, "init", "");
    }
    public void updateRoomState(String roomId, String lastMessageId){
        String key = ROOM_STATE_KEY_PREFIX + roomId + ":state";
        SRT.opsForHash().put(key, "lastMessageId", lastMessageId);
    }
    public Optional<String> getLastMessageId(String roomId){
        String key = ROOM_STATE_KEY_PREFIX + roomId + ":state";
        Object lastMessageId = SRT.opsForHash().get(key, "lastMessageId");
        return Optional.ofNullable(lastMessageId).map(Object::toString);
    }
    public void deleteRoomState(String roomId){
        String key = ROOM_STATE_KEY_PREFIX + roomId + ":state";
        SRT.delete(key);
    }
}
