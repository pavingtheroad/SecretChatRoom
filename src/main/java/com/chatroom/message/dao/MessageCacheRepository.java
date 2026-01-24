package com.chatroom.message.dao;

import com.chatroom.message.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@Repository
// 管理缓存持久化
public class MessageCacheRepository {
    private static final Logger log = LoggerFactory.getLogger(MessageCacheRepository.class);
    private final StringRedisTemplate redisTemplate;
    public MessageCacheRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存消息 Redis自动生成id
     * 无条件信任来自服务层的参数正确性
     */
    public RecordId saveMessage(Message message){
        String roomId = message.roomId().toString();
        String streamKey = "chat:room:" + roomId + ":msg";
        String userPKId = message.senderId().toString();
        HashMap<String, Object> value = new HashMap<>();
        value.put("userId", userPKId);
        value.put("type", message.type().name());
        value.put("content", message.content());
        value.put("timestamp", message.createdAt());
        return redisTemplate.opsForStream().add(streamKey, value);
    }

    /**
     * 倒序分页查询
     * @param roomId, range, limit
     */
    public List<MapRecord<String, Object, Object>> reverseRangeMessages(Long roomId, Range range, Limit limit){
        String streamKey = "chat:room:" + roomId + ":msg";
        return redisTemplate.opsForStream().reverseRange(streamKey, range, limit);

    }
    /**
     * 顺序分页查询
     * @param roomId, range, limit
     */
    public List<MapRecord<String, Object, Object>> rangeMessages(Long roomId, Range range, Limit limit){
        String streamKey = "chat:room:" + roomId + ":msg";
        return redisTemplate.opsForStream().range(streamKey, range, limit);
    }
    /**
     * 消息剪枝
     * @param roomId, cutoffId
     */
    public void trimMessage(String roomId, String cutoffId){
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
     * 获取最新消息ID
     * @param roomId
     */
    public String getLastMessageId(Long roomId){
        String streamKey = "chat:room:" + roomId + ":msg";
        List<MapRecord<String, Object, Object>> rec =
                redisTemplate.opsForStream().reverseRange(streamKey, Range.unbounded(), Limit.limit().count(1));
        return rec.isEmpty() ? null : rec.get(0).getId().getValue();
    }
}