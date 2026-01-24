package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.dao.MessageCursorRepository;
import com.chatroom.room.dao.RoomCacheRepository;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.SortParameters;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class MessageTrimService {
    private final MessageCacheRepository messageCacheRepository;
    public final RoomCacheRepository roomCacheRepository;
    public MessageTrimService(MessageCacheRepository messageCacheRepository,
                              RoomCacheRepository roomCacheRepository) {
        this.messageCacheRepository = messageCacheRepository;
        this.roomCacheRepository = roomCacheRepository;
    }
    public Long computeCutOffTime(String roomId){
        Long ttlMillis = roomCacheRepository.getRoomInfo(roomId).ttlMillis();
        Long now = Instant.now().toEpochMilli();
        return now - ttlMillis;
    }
    public Optional<String> findCutOffStreamId(String roomId,Long cutoffTime){
        String startId = "-";
        int batchSize = 100;
        boolean done = false;
        while (!done) {
            List<MapRecord<String, Object, Object>> records = messageCacheRepository.rangeMessages(roomId,
                    Range.from(Range.Bound.inclusive(startId)).to(Range.Bound.unbounded()),
                    Limit.limit().count(batchSize));
            if (records.isEmpty())
                return Optional.empty();
            for (MapRecord<String, Object, Object> record : records) {
                Object timestamp = record.getValue().get("timestamp");
                if (timestamp == null){
                    continue;
                }
                long messageTime = Long.parseLong(timestamp.toString());
                if (messageTime >= cutoffTime){     // 找到第一个大于cutoffTime的消息
                    return Optional.of(record.getId().getValue());
                }
            }
            MapRecord<String, Object, Object> lastRecord = records.get(records.size() - 1);
            startId = "(" + lastRecord.getId().getValue();
            if (records.size() < batchSize) {
                done = true;
            }
        }
        return Optional.empty();
    }

    public void trimRoomMessages(String roomId){
        Long cutoffTime = computeCutOffTime(roomId);
        Optional<String> lastStreamId = messageCacheRepository.getLastMessageId(roomId);
        if (lastStreamId.isEmpty())
            return;
        Optional<String> cutoffStreamId = findCutOffStreamId(roomId, cutoffTime);
        String streamId = cutoffStreamId.orElse(lastStreamId.get());
        messageCacheRepository.trimMessage(roomId, streamId);
    }
}
