package com.chatroom.message.dao;

import com.chatroom.message.dto.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@Repository
@RequiredArgsConstructor
// 管理缓存持久化
public class MessageCacheRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageCacheRepository.class);
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 保存消息 Redis自动生成id
     * @param messageDto
     */
    public String saveMessage(MessageDTO messageDto){
        String roomId = messageDto.getRoomId();
        if (roomId.isEmpty() || roomId == null)
            throw new RuntimeException("roomId is empty for saving message");

        String streamKey = "chat:room:" + messageDto.getRoomId() + ":msg";

        String userPKId = messageDto.getUserPKId();
        if (userPKId == null || userPKId.isEmpty()){
            throw new RuntimeException("userId is empty for saving message");
        }
        HashMap<String, Object> value = new HashMap<>();
        value.put("Id", userPKId);
        value.put("timestamp", String.valueOf(messageDto.getTimestamp()));
        value.put("type", messageDto.getType());
        value.put("content", messageDto.getContent());
        try {
            Object streamId = redisTemplate.opsForStream().add(streamKey, value);
            return streamId.toString();
        } catch (Exception e) {
            throw new RuntimeException("save message failed: " + e.getMessage(), e);
        }
    }

    /**
     * Range/Revrange分页查询
     * @param roomId, range, limit, reverse(1: 倒序, 0: 正序)
     */
    public List<MapRecord<String, Object, Object>> rangeMessages(String roomId, Range range, Limit limit, boolean reverse){
        String streamKey = "chat:room:" + roomId + ":msg";
        try{
            return reverse? redisTemplate.opsForStream().reverseRange(streamKey, range, limit) :
                    redisTemplate.opsForStream().range(streamKey, range, limit);
        } catch (Exception e) {
            throw new RuntimeException("range messages failed: " + e.getMessage(), e);
        }
    }
    /**
     * 消息剪枝
     * @param roomId, cutoffId
     */
    public void trimMessage(String roomId, String cutoffId){

        // 前置校验，检查cutoffId是否合法

        String streamKey = "chat:room:" + roomId + ":msg";
        Long deleted = redisTemplate.execute((RedisConnection connection) ->
                (Long)connection.execute(
                        "XTRIM",
                        streamKey.getBytes(StandardCharsets.UTF_8),
                        "MINID".getBytes(),
                        cutoffId.getBytes()
                )
        );
        log.info("trim {} entries in room {}", deleted, roomId);
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
     * 获取游标（不存在时返回最新）
     * @param roomId, userPKId
     */
    public String getCursor(String roomId, String userPKId){
        String cursorKey = "chat:room:" + roomId + ":userPKId:" + userPKId;
        Object cursorId = redisTemplate.opsForHash().get(cursorKey, "cursor");
        return cursorId != null ? cursorId.toString() : "$";
    }
    /**
     * 获取最新消息ID
     * @param roomId
     */
    public String getLastMessageId(String roomId){
        String streamKey = "chat:room:" + roomId + ":msg";
        List<MapRecord<String, Object, Object>> rec =
                redisTemplate.opsForStream().reverseRange(streamKey, Range.unbounded(), Limit.limit().count(1));
        return rec.isEmpty() ? null : rec.get(0).getId().getValue();
    }
}