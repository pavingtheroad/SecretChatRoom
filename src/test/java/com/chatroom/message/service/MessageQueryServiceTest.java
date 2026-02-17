package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.dao.MessageCursorRepository;
import com.chatroom.message.dto.MessageDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {
    @Mock
    private MessageCacheRepository messageCacheRepository;

    @Mock
    private MessageCursorRepository messageCursorRepository;

    @InjectMocks
    private MessageQueryService messageQueryService;

    @Test
    void initMessageQuery_shouldReturnMessages_whenNoCursorExists(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 不存在
        when(messageCursorRepository.getCursor(roomId, userId))
                .thenReturn(Optional.empty());

        // mock 最新消息ID存在
        when(messageCacheRepository.getLastMessageId(roomId))
                .thenReturn(Optional.of("1000-0"));

        // mock Redis返回记录
        MapRecord<String, String, String> record =
                StreamRecords.newRecord()
                        .in(roomId)
                        .ofMap(Map.of(
                                "senderId", "user1",
                                "type", "TEXT",
                                "content", "hello",
                                "createdAt", "123456"
                        ))
                        .withId(RecordId.of("1000-0"));

        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(List.of(record));

        // 执行
        List<MessageDTO> result =
                messageQueryService.initMessageQuery(roomId, userId, limit);

        // 断言
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
}
