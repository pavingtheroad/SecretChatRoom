package com.chatroom.message.dao;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class MessageCursorRepository {
    private final StringRedisTemplate redisTemplate;
    public MessageCursorRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    /**
     * 游标更新
     * @param roomId, userPKId, cursorId
     */
    public void updateCursor(String roomId, String userPKId, String cursorId){
        String cursorKey = "chat:room:" + roomId + ":userPKId:" + userPKId;
        redisTemplate.opsForHash().put(cursorKey, "cursor", cursorId);
    }
    /**
     * 获取游标
     * @param roomId, userPKId
     */
    public Optional<String> getCursor(String roomId, String userPKId){
        String cursorKey = "chat:room:" + roomId + ":userPKId:" + userPKId;
        return Optional.ofNullable((String)redisTemplate.opsForHash().get(cursorKey, "cursor"));
    }
}
