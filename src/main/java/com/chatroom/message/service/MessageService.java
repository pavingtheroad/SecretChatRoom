// 消息生命周期管理
package com.chatroom.message.service;

import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.room.service.RoomManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    private final RoomManagementService roomManagementService;
    private final MessageCacheRepository messageCacheRepository;

    /**
     * 进入房间后的消息查询初始化（从最新消息向上查询）
     * @param roomId 提供room信息
     * @param limit 查询数量
     */
    public List<MessageDTO> initMessageQuery(String roomId, int limit){
        String lastMessageId = messageCacheRepository.getLastMessageId(roomId);
        Range<String> range = Range.leftUnbounded(Range.Bound.inclusive(lastMessageId));
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records = messageCacheRepository.rangeMessages(roomId, range, lim, true);
        return convertToMessageDTO(records);
    }
    /**
     * 分页查询更早消息
     * @param roomId, start(当前页面的最旧Id), limit
     */
    public List<MessageDTO> getForwardMessages(String roomId, String start, int limit){
        Range<String> range = Range.leftUnbounded(Range.Bound.exclusive(start));
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records = messageCacheRepository.rangeMessages(roomId, range, lim, true);
        return convertToMessageDTO(records);
    }
    /**
     * 分页查询更晚消息
     * @param roomId, end(当前页面的最新Id), limit
     */
    public List<MessageDTO> getBackwardMessages(String roomId, String end, int limit){
        Range<String> range = Range.rightUnbounded(Range.Bound.exclusive(end));
        Limit lim = Limit.limit().count(limit);
        List<MapRecord<String, Object, Object>> records = messageCacheRepository.rangeMessages(roomId, range, lim, false);
        return convertToMessageDTO(records);
    }
    /**
     * 保存并广播消息
     * @param message 消息对象
     * @param roomId 房间ID
     */
    public void saveAndBroadcast(MessageDTO message, String roomId){

    }

    /**
     * 剪枝（根据房间TTL设置进行剪枝）
     * @Scheduled周期性任务
     */
    @Scheduled(fixedDelay = 60000)    // 周期60秒
    public void pruneExpiredMessages(){
        /*
        get all roomIds
        for each roomId :
            get roomTTL
            cutoffId = (System.currentTimeMillis() - ttlMillis) + "-0";
            XTRIM MINID=cutoffId
         */
    }

    // 将RedisRecord转换为MessageDTO
    private List<MessageDTO> convertToMessageDTO(List<MapRecord<String, Object, Object>> records){
        return records.stream()
                .map(record -> {
                    Map<Object, Object> v = record.getValue();
                    MessageDTO dto = new MessageDTO();
                    dto.setUserId((String) v.get("userId"));
                    dto.setTimestamp(Long.parseLong(v.get("timestamp").toString()));
                    dto.setType((String) v.get("type"));
                    dto.setContent((String) v.get("content"));
                    return dto;
                })
                .toList();
    }
    // Stream id比较 <ms>-<seq>
    private boolean isLess(String id1, String id2){
        return compare(id1, id2) < 0;
    }
    private int compare(String id1, String id2){
        String[] id1s = id1.split("-");
        String[] id2s = id2.split("-");
        int t = Long.compare(Long.parseLong(id1s[0]), Long.parseLong(id2s[0]));    // (x < y) ? -1 : ((x == y) ? 0 : 1);
        if (t != 0){
            return t;
        }
        return Long.compare(Long.parseLong(id1s[1]), Long.parseLong(id2s[1]));
    }

}