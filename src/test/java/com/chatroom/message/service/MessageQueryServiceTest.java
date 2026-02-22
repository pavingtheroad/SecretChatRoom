package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.dao.MessageCursorRepository;
import com.chatroom.message.dto.MessageDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MessageQueryServiceTest {
    @Mock
    private MessageCacheRepository messageCacheRepository;

    @Mock
    private MessageCursorRepository messageCursorRepository;

    @InjectMocks
    private MessageQueryService messageQueryService;

    @Test    // 有Cursor，且能找到对应消息
    void initMessageQuery_shouldReturnMessages_whenCursorExists(){
        String roomId = "room1";
        String userId = "user1";
        String streamKey = "chat:room:";
        int limit = 10;
        // mock cursor 存在
        when(messageCursorRepository.getCursor(roomId, userId))
                .thenReturn(Optional.of("1999-0"));

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "1999-0", "hello");

        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(List.of(record1));
        // 执行
        List<MessageDTO> result =
                messageQueryService.initMessageQuery(roomId, userId, limit);

        // 验证
        ArgumentCaptor<Range<String>> rangeCaptor = ArgumentCaptor.forClass(Range.class);
        verify(messageCacheRepository, times(1))
                .reverseRangeMessages(
                        eq(roomId),
                        rangeCaptor.capture(),
                        any()
                );
        Range<String> range = rangeCaptor.getValue();
        assertEquals("1999-0", range.getUpperBound().getValue().get());
        verify(messageCursorRepository, never())
                .updateCursor(any(), any(), any());
        // 断言，验证返回结果
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    @Test    // 无Cursor但存在最新消息ID
    void initMessageQuery_shouldReturnMessages_whenNoCursorExists(){
        String roomId = "room1";
        String userId = "user1";
        String streamKey = "chat:room:";
        int limit = 10;
        // mock cursor 不存在
        when(messageCursorRepository.getCursor(roomId, userId))
                .thenReturn(Optional.empty());

        // mock 最新消息ID存在
        when(messageCacheRepository.getLastMessageId(roomId))
                .thenReturn(Optional.of("2000-0"));

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "2000-0", "hello");

        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(List.of(record1));

        // 执行
        List<MessageDTO> result =
                messageQueryService.initMessageQuery(roomId, userId, limit);

        // 验证返回结果
        ArgumentCaptor<Range<String>> rangeCaptor = ArgumentCaptor.forClass(Range.class);    // 捕获参数Range, 预期为从2000-0开始
        verify(messageCacheRepository, times(1))
                .reverseRangeMessages(
                        eq(roomId),
                        rangeCaptor.capture(),
                        any()
                );
        Range<String> range = rangeCaptor.getValue();
        assertEquals("2000-0", range.getUpperBound().getValue().get());
        verify(messageCursorRepository, times(1))
                .updateCursor(roomId, userId, "2000-0");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    @Test    // 有Cursor，但无对应消息; 有最新消息
    void initMessageQuery_shouldReturnMessages_whenCursorExistsButNoMatchingMessages(){
        String roomId = "room1";
        String userId = "user1";
        String streamKey = "chat:room:";
        int limit = 10;
        // mock cursor 存在
        when(messageCursorRepository.getCursor(roomId, userId))
                .thenReturn(Optional.of("1999-0"));
        // mock 最新消息ID存在
        when(messageCacheRepository.getLastMessageId(roomId))
                .thenReturn(Optional.of("2000-0"));

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "2000-0", "hello");

        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                argThat(range ->
                        range.getUpperBound().getValue().get().equals("1999-0")
                ),
                any()
        )).thenReturn(Collections.emptyList());
        // 第一次返回空列表，第二次返回有消息
        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(Collections.emptyList())
                .thenReturn(List.of(record1));
        // 执行
        List<MessageDTO> result = messageQueryService.initMessageQuery(roomId, userId, limit);
        ArgumentCaptor<Range<String>> rangeCaptor = ArgumentCaptor.forClass(Range.class);    // 捕获参数Range, 预期为从2000-0开始
        verify(messageCacheRepository, times(2))
                .reverseRangeMessages(
                        eq(roomId),
                        rangeCaptor.capture(),
                        any()
                );
        List<Range<String>> ranges = rangeCaptor.getAllValues();
        assertEquals("1999-0",
                ranges.get(0).getUpperBound().getValue().get());

        assertEquals("2000-0",
                ranges.get(1).getUpperBound().getValue().get());
        verify(messageCursorRepository, times(1))
                .updateCursor(roomId, userId, "2000-0");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    private MapRecord<String, Object, Object> buildRecord(String roomId, String id, String content){
        Map<Object, Object> contentMap = new HashMap<>();
        contentMap.put("senderId", "user1");
        contentMap.put("type", "TEXT");
        contentMap.put("content", content);
        contentMap.put("createdAt", "123456");
        return StreamRecords.newRecord()
                .in(roomId)
                .ofMap(contentMap)
                .withId(RecordId.of(id));
    }
}
