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
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    // 测试initMessageQuery
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

        // 验证cursor未变化
        verify(messageCursorRepository, never())
                .updateCursor(any(), any(), any());
        // 用户能看到消息
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
        verifyCursorUpdate(roomId, userId, "2000-0", 1);
        assertEquals(1, result.size());
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
                .reverseRangeMessages( eq(roomId), rangeCaptor.capture(), any() );
        List<Range<String>> ranges = rangeCaptor.getAllValues();
        assertEquals("1999-0", ranges.get(0).getUpperBound().getValue().get());
        assertEquals("2000-0", ranges.get(1).getUpperBound().getValue().get());
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

        assertEquals(0, result.size());
    }
    @Test    // Cursor存在但无效，lastId存在但无效
    void initMessageQuery_shouldReturnEmptyList_whenCursorInvalidAndLastMessageInvalid(){
        String roomId = "room1";
        String userId = "user1";
        int limit = 10;
        // mock cursor 存在，但无效
        givenCursor(roomId, userId, "1999-0");
        // mock 最新消息ID存在，但无效
        givenLastMessageId(roomId, "2000-0");
        givenReverseReturns(roomId,
                Collections.emptyList(),    // 第一次通过Cursor查询无效
                Collections.emptyList()     // 第两次通过lastId查询无效
        );
        // 执行
        List<MessageDTO> result = messageQueryService.initMessageQuery(roomId, userId, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, null, 0);
        assertTrue(result.isEmpty());
    }
    // 测试getForwardMessages
    @Test
    void getForwardMessages_shouldReturnEmptyList_whenNoOlderMessages(){
        String roomId = "room1";
        String userId = "user1";
        String end = "2000-0";
        int limit = 10;
        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(Collections.emptyList());
        // 执行
        List<MessageDTO> result = messageQueryService.getForwardMessages(roomId, userId, end, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, null, 0);
        assertEquals(0, result.size());
    }
    @Test
    void getForwardMessages_shouldReturnMessages_whenOlderMessagesFound(){
        String roomId = "room1";
        String userId = "user1";
        String end = "2000-0";
        int limit = 10;
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "1999-0", "hello");
        MapRecord<String, Object, Object> record2 = buildRecord(roomId, "2000-0", "world");
        List<MapRecord<String, Object, Object>> records = new ArrayList<>();
        records.add(record2);
        records.add(record1);
        when(messageCacheRepository.reverseRangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(records);
        // 执行
        List<MessageDTO> result = messageQueryService.getForwardMessages(roomId, userId, end, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, "1999-0", 1);
        assertEquals(2, result.size());
    }
    // 测试getBackwardMessages
    @Test    // 无newer msg，根据当前页面最新id无法继续向后查询消息
    void getBackwardMessages_shouldReturnEmptyList_whenNoNewerMessages(){
        String roomId = "room1";
        String userId = "user1";
        String start = "2000-0";
        int limit = 10;
        when(messageCacheRepository.rangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(Collections.emptyList());
        // 执行
        List<MessageDTO> result = messageQueryService.getBackwardMessages(roomId, userId, start, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, null, 0);
        assertEquals(0, result.size());
    }
    @Test    // 查询到更晚消息
    void getBackwardMessages_shouldReturnMessages_whenNewerMessagesFound(){
        String roomId = "room1";
        String userId = "user1";
        String start = "2000-0";
        int limit = 10;
        MapRecord<String, Object, Object> record1 = buildRecord(roomId, "2001-0", "hello");
        MapRecord<String, Object, Object> record2 = buildRecord(roomId, "2002-0", "world");
        List<MapRecord<String, Object, Object>> records = new ArrayList<>();
        records.add(record1);
        records.add(record2);
        when(messageCacheRepository.rangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(records);
        // 执行
        List<MessageDTO> result = messageQueryService.getBackwardMessages(roomId, userId, start, limit);
        // 验证
        verifyCursorUpdate(roomId, userId, "2002-0", 1);
        assertEquals(2, result.size());
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
    @SafeVarargs
    private void givenReverseReturns(String roomId, List<MapRecord<String, Object, Object>>... results){
        OngoingStubbing<List<MapRecord<String, Object, Object>>> stubbing =
                when(messageCacheRepository.reverseRangeMessages(
                        eq(roomId),
                        any(),
                        any()
                ));
        for (List<MapRecord<String, Object, Object>> result : results) {
            stubbing = stubbing.thenReturn(result);
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
