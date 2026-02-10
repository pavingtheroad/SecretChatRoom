package com.chatroom.message.dao;

import com.chatroom.message.entity.ChatMessage;
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
import java.util.Optional;

/**
 * 消息缓存仓库
 * - saveMessage: 将消息保存到指定聊天室的Redis流中，返回生成的消息ID
 * - reverseRangeMessages: 从指定聊天室按倒序分页查询消息列表
 * - rangeMessages: 从指定聊天室按正序分页查询消息列表
 * - trimMessage: 对指定聊天室的消息进行剪枝，删除早于截止ID的消息
 * - getLastMessageId: 获取指定聊天室的最新消息ID
 */
@Repository
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
    public RecordId saveMessage(ChatMessage chatMessage){
        String roomId = chatMessage.roomId();
        String streamKey = "chat:room:" + roomId + ":msg";
        String userPKId = chatMessage.senderId();
        HashMap<String, Object> value = new HashMap<>();
        value.put("userId", userPKId);
        value.put("type", chatMessage.type().name());
        value.put("content", chatMessage.content());
        value.put("timestamp", chatMessage.createdAt());
        return redisTemplate.opsForStream().add(streamKey, value);
    }

    /**
     * 倒序分页查询
     * @param roomId, range, limit
     */
    public List<MapRecord<String, Object, Object>> reverseRangeMessages(String roomId, Range range, Limit limit){
        String streamKey = "chat:room:" + roomId + ":msg";
        return redisTemplate.opsForStream().reverseRange(streamKey, range, limit);

    }
    /**
     * 顺序分页查询
     * @param roomId, range, limit
     */
    public List<MapRecord<String, Object, Object>> rangeMessages(String roomId, Range range, Limit limit){
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
    public Optional<String> getLastMessageId(String roomId){
        String streamKey = "chat:room:" + roomId + ":msg";
        List<MapRecord<String, Object, Object>> rec =
                redisTemplate.opsForStream().reverseRange(streamKey, Range.unbounded(), Limit.limit().count(1));
        if (rec == null || rec.isEmpty()){
            return Optional.empty();
        }
        return Optional.of(rec.get(0).getId().getValue());
    }
}