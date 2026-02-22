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
import org.mockito.stubbing.OngoingStubbing;
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
    // 测试initMessageQuery，5种路径
    @Test    // 有Cursor，且能找到对应消息
    void initMessageQuery_shouldReturnMessages_whenCursorExists(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 存在
        givenCursor(roomId, userId, "1999-0");

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "1999-0", "hello");

        givenReverseReturns(roomId, List.of(record1));

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
        verifyCursorUpdate(roomId, userId, "1999-0", 0);
        // 断言，验证返回结果
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    @Test    // 无Cursor但存在最新消息ID
    void initMessageQuery_shouldReturnMessages_whenNoCursorExists(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 不存在
        givenCursor(roomId, userId, null);

        // mock 最新消息ID存在
        givenLastMessageId(roomId, "2000-0");

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "2000-0", "hello");

        givenReverseReturns(roomId, List.of(record1));

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
        verifyCursorUpdate(roomId, userId, "2000-0", 1);
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    @Test    // 有Cursor，但无对应消息; 有最新消息
    void initMessageQuery_shouldReturnMessages_whenCursorExistsButNoMatchingMessages(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 存在
        givenCursor(roomId, userId, "1999-0");
        // mock 最新消息ID存在
        givenLastMessageId(roomId, "2000-0");

        // mock Redis返回记录
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "2000-0", "hello");

        // 第一次返回空列表，第二次返回有消息
        givenReverseReturns(roomId, Collections.emptyList(), List.of(record1));
        // 执行
        List<MessageDTO> result = messageQueryService.initMessageQuery(roomId, userId, limit);

        // 捕获参数Range, 预期为从2000-0开始
        ArgumentCaptor<Range<String>> rangeCaptor = ArgumentCaptor.forClass(Range.class);
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

        verifyCursorUpdate(roomId, userId, "2000-0", 1);

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }
    @Test    // 无Cursor，无最新消息ID
    void initMessageQuery_shouldReturnEmptyList_whenNoCursorAndNoLastMessageId(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 不存在
        givenCursor(roomId, userId, null);
        // mock 最新消息ID不存在
        givenLastMessageId(roomId, null);
        // 执行
        List<MessageDTO> result = messageQueryService.initMessageQuery(roomId, userId, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, null, 0);
        verify(messageCacheRepository, never())
                .reverseRangeMessages(
                        any(),
                        any(),
                        any()
                );
        assertEquals(0, result.size());
    }
    @Test    // Cursor无效，无最新消息Id
    void initMessageQuery_shouldReturnEmptyList_whenCursorInvalidAndNoLastMessage(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 存在，但无效
        givenCursor(roomId, userId, "1999-0");
        // mock 最新消息ID不存在
        givenLastMessageId(roomId, null);
        givenReverseReturns(roomId, Collections.emptyList());
        // 执行
        List<MessageDTO> result = messageQueryService.initMessageQuery(roomId, userId, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, null, 0);
        verify(messageCacheRepository, times(1))
                .reverseRangeMessages(
                        eq(roomId),
                        any(),
                        any()
                );
        assertEquals(0, result.size());
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
    private void givenReverseReturns(String roomId, List<?>... results){
        OngoingStubbing<List<MapRecord<String, Object, Object>>> stubbing =
                when(messageCacheRepository.reverseRangeMessages(
                        eq(roomId),
                        any(),
                        any()
                ));
        for (List<?> result : results) {
            stubbing = stubbing.thenReturn((List<MapRecord<String, Object, Object>>) result);
        }
    }
    private void verifyCursorUpdate(String roomId, String userId, String expectedCursor, int times){
        verify(messageCursorRepository, times(times))
                .updateCursor(roomId, userId, expectedCursor);
    }
    private void givenCursor(String roomId, String userId, String cursor){
        when(messageCursorRepository.getCursor(roomId, userId))
                .thenReturn(Optional.ofNullable(cursor));
    }
    private void givenLastMessageId(String roomId, String lastMessageId){
        when(messageCacheRepository.getLastMessageId(roomId))
                .thenReturn(Optional.ofNullable(lastMessageId));
    }
}
