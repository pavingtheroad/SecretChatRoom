package com.chatroom.message.dao;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 消息游标仓库
 * - updateCursor: 更新指定用户在特定聊天室中的消息读取游标位置
 * - getCursor: 获取指定用户在特定聊天室中的消息读取游标位置
 */
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
        String cursorKey = "chat:room:" + userPKId + ":cursor";
        redisTemplate.opsForHash().put(cursorKey, roomId, cursorId);
    }
    /**
     * 获取游标
     * @param roomId, userPKId
     */
    public Optional<String> getCursor(String roomId, String userPKId){
        String cursorKey = "chat:room:" + userPKId + ":cursor";
        Object value = redisTemplate.opsForHash().get(cursorKey, roomId);
        return Optional.ofNullable(value).map(Object::toString);
    }
}
