package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.dao.MessageCursorRepository;
import com.chatroom.message.dto.MessageDTO;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MessageQueryService {
    private final MessageCacheRepository messageCacheRepository;
    private final MessageCursorRepository messageCursorRepository;
    public MessageQueryService(MessageCacheRepository messageCacheRepository, MessageCursorRepository messageCursorRepository){
        this.messageCacheRepository = messageCacheRepository;
        this.messageCursorRepository = messageCursorRepository;
    }
    /**
     * 进入房间后的消息查询初始化（从最新消息向上查询）
     * @param roomId 提供room信息
     * @param limit 查询数量
     */
    public List<MessageDTO> initMessageQuery(String roomId, String userPKId, int limit){
        Optional<String> cursorOpt = messageCursorRepository.getCursor(roomId, userPKId);   // 寻找上次查看的游标
        String startId = cursorOpt.orElseGet(() ->
                messageCacheRepository.getLastMessageId(roomId).orElse(null)
        );
        if (startId == null){
            return Collections.emptyList();
        }
        Range<String> range = Range.leftUnbounded(Range.Bound.inclusive(startId));      // 从cursor向前查询
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records = messageCacheRepository.reverseRangeMessages(roomId, range, lim);
        // cursor失效后的兜底
        if (records.isEmpty()) {
            Optional<String> lastId = messageCacheRepository.getLastMessageId(roomId);
            if (lastId.isEmpty()) {
                return Collections.emptyList();
            }

            range = Range.leftUnbounded(Range.Bound.inclusive(lastId.get()));
            records = messageCacheRepository.reverseRangeMessages(roomId, range, lim);
        }
        Collections.reverse(records);
        MapRecord<String, Object, Object> last = records.get(records.size() - 1);
        messageCursorRepository.updateCursor(roomId, userPKId, last.getId().getValue());    // cursor同步到最新消息
        return convertToMessageDTO(records);
    }
    /**
     * 分页查询更早消息
     * @param roomId, start(当前页面的最旧Id), limit
     */
    public List<MessageDTO> getForwardMessages(String roomId,
                                               String userPKId,
                                               String start,
                                               int limit) {
        Range<String> range = Range.leftUnbounded(Range.Bound.exclusive(start));
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.reverseRangeMessages(roomId, range, lim);
        if (records.isEmpty()) {
            return Collections.emptyList();
        }
        Collections.reverse(records);
        MapRecord<String, Object, Object> oldest = records.get(0);
        messageCursorRepository.updateCursor(roomId, userPKId, oldest.getId().getValue());
        return convertToMessageDTO(records);
    }
    /**
     * 分页查询更晚消息
     * @param roomId, end(当前页面的最新Id), limit
     */
    public List<MessageDTO> getBackwardMessages(String roomId,
                                                String userPKId,
                                                String end,
                                                int limit) {
        Range<String> range = Range.rightUnbounded(Range.Bound.exclusive(end));
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.rangeMessages(roomId, range, lim);
        if (!records.isEmpty()) {
            MapRecord<String, Object, Object> newest = records.get(records.size() - 1);
            messageCursorRepository.updateCursor(roomId, userPKId, newest.getId().getValue());
        }
        return convertToMessageDTO(records);
    }
    // 将RedisRecord转换为MessageDTO
    private List<MessageDTO> convertToMessageDTO(List<MapRecord<String, Object, Object>> records){
        return records.stream()
                .map(record -> {
                    Map<Object, Object> v = record.getValue();
                    MessageDTO dto = new MessageDTO();
                    dto.setUserPKId((String) v.get("userId"));
                    dto.setTimestamp(Long.parseLong(v.get("timestamp").toString()));
                    dto.setType((String) v.get("type"));
                    dto.setContent((String) v.get("content"));
                    return dto;
                })
                .toList();
    }
}
