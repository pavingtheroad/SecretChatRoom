package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.room.dao.RoomCacheRepository;
import com.chatroom.room.dto.RoomInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class MessageTrimServiceTest {
    @Mock
    private MessageCacheRepository messageCacheRepository;
    @Mock
    private RoomCacheRepository roomCacheRepository;
    @InjectMocks
    private MessageTrimService messageTrimService;
    @Spy
    @InjectMocks
    private MessageTrimService spiedMessageTrimService;
    @BeforeEach
    void setUp(){
        reset(spiedMessageTrimService);
    }
    @Test
    void computeCutOffTime(){
        String roomId = "room1";
        RoomInfo roomInfo = new RoomInfo("room1",
                null,
                null,
                null,
                null,
                null,
                null,
                10000L);
        when(roomCacheRepository.getRoomInfo(roomId))
                .thenReturn(roomInfo);
        long before = Instant.now().toEpochMilli();

        Long cutoff = messageTrimService.computeCutOffTime(roomId);

        long after = Instant.now().toEpochMilli();

        assertTrue(cutoff <= after - 10000L);
        assertTrue(cutoff >= before - 10000L);
    }
    // 测试findCutOffStreamId
    @Test    // 无消息
    void findCutOffStreamId_noMessages(){
        String roomId = "room1";
        Long cutoffTime = 10L;
        givenRangeMessagesReturn(roomId, Collections.emptyList());
        Optional<String> result = messageTrimService.findCutOffStreamId(roomId, cutoffTime);
        assertTrue(result.isEmpty());
    }
    @Test    // 第一次循环找到
    void findCutoffStreamId_findOutInFirstLoop(){
        String roomId = "room1";
        Long cutoffTime = 10L;
        MapRecord<String, Object, Object> message = buildMessageRecord(roomId, "1-0", 20L);
        List<MapRecord<String, Object, Object>> messages = List.of(message);
        givenRangeMessagesReturn(roomId, messages);
        Optional<String> result = messageTrimService.findCutOffStreamId(roomId, cutoffTime);
        assertTrue(result.isPresent());
        assertEquals("1-0", result.get());
    }
    @Test    // 第二批找到
    void findCutoffStreamId_findOutInSecondLoop(){
        String roomId = "room1";
        Long cutoffTime = 10L;
        MapRecord<String, Object, Object> message = buildMessageRecord(roomId, "1-0", 9L);
        MapRecord<String, Object, Object> message2 = buildMessageRecord(roomId, "1-1", 15L);
        List<MapRecord<String, Object, Object>> messages = new ArrayList<>();
        List<MapRecord<String, Object, Object>> messages2 = new ArrayList<>();
        messages.add(message);
        messages2.add(message2);
        messages.addAll(Collections.nCopies(99, buildMessageRecord(null, null, 8L)));
        messages2.addAll(Collections.nCopies(99, buildMessageRecord(null, null, 8L)));
        givenRangeMessagesReturn(roomId, messages, messages2);
        Optional<String> result = messageTrimService.findCutOffStreamId(roomId, cutoffTime);
        assertTrue(result.isPresent());
        assertEquals("1-1", result.get());
    }
    @Test    // 多次循环无结果
    void findCutoffStreamId_noResult(){
        String roomId = "room1";
        Long cutoffTime = 10L;
        MapRecord<String, Object, Object> message = buildMessageRecord(roomId, "1-0", 9L);
        MapRecord<String, Object, Object> message2 = buildMessageRecord(roomId, "1-1", 8L);
        List<MapRecord<String, Object, Object>> messages = new ArrayList<>();
        List<MapRecord<String, Object, Object>> messages2 = new ArrayList<>();
        messages.add(message);
        messages2.add(message2);
        messages.addAll(Collections.nCopies(99, buildMessageRecord(null, null, 8L)));
        messages2.addAll(Collections.nCopies(9, buildMessageRecord(null, null, 8L)));
        givenRangeMessagesReturn(roomId, messages, messages2);
        Optional<String> result = messageTrimService.findCutOffStreamId(roomId, cutoffTime);
        assertTrue(result.isEmpty());

    }
    // 测试trimRoomMessages
    @Test    // 无消息
    void trimRoomMessages_noMessages(){
        String roomId = "room1";
        RoomInfo roomInfo = new RoomInfo("room1",
                null,
                null,
                null,
                null,
                null,
                null,
                10000L);
        when(roomCacheRepository.getRoomInfo(roomId))
                .thenReturn(roomInfo);
        givenGetLastMessageIdReturn(roomId, null);
        messageTrimService.trimRoomMessages(roomId);
        verify(messageCacheRepository, never())
                .trimMessage(any(), any());
    }
    @Test    // 最新消息存在，没有剪枝Id
    void trimRoomMessages_messageExists_noCutoffId(){
        String roomId = "room1";
        givenGetLastMessageIdReturn(roomId, "2000-0");
        RoomInfo roomInfo = new RoomInfo("room1",
                null,
                null,
                null,
                null,
                null,
                null,
                10000L);
        when(roomCacheRepository.getRoomInfo(roomId))
                .thenReturn(roomInfo);
        givenRangeMessagesReturn(roomId, Collections.emptyList());

        messageTrimService.trimRoomMessages(roomId);
        verify(messageCacheRepository, times(1))
                .trimMessage(roomId, "2000-0");
    }
    @Test    // 最新消息存在，有剪枝Id
    void trimRoomMessages_messageExists_withCutoffId(){
        String roomId = "room1";
        givenGetLastMessageIdReturn(roomId, "2000-0");
        RoomInfo roomInfo = new RoomInfo(
                roomId,
                null, null, null, null, null, null,
                1000L
        );
        when(roomCacheRepository.getRoomInfo(roomId))
                .thenReturn(roomInfo);

        long now = Instant.now().toEpochMilli();
        long messageTime = now - 500;  // 一定 >= cutoffTime
        MapRecord<String, Object, Object> cutoffRecord =
                buildMessageRecord(roomId, "1500-0", messageTime);

        when(messageCacheRepository.rangeMessages(
                eq(roomId),
                any(),
                any()
        )).thenReturn(List.of(cutoffRecord));
        messageTrimService.trimRoomMessages(roomId);
        verify(messageCacheRepository)
                .trimMessage(roomId, "1500-0");
    }
    @SafeVarargs
    private void givenRangeMessagesReturn(String roomId, List<MapRecord<String, Object, Object>>... messages){
        OngoingStubbing<List<MapRecord<String, Object, Object>>> stubbing = when(
                messageCacheRepository.rangeMessages(
                        eq(roomId),
                        any(),
                        any()
                )
        );
        for (List<?> message : messages){
            stubbing = stubbing.thenReturn((List<MapRecord<String, Object, Object>>) message);
        }
    }
    private void givenGetLastMessageIdReturn(String roomId, String lastMessageId){
        when(messageCacheRepository.getLastMessageId(roomId))
                .thenReturn(Optional.ofNullable(lastMessageId));
    }
    private MapRecord<String, Object, Object> buildMessageRecord(String roomId, String recordId, Long timestamp){
        Map<Object, Object> value = new HashMap<>();
        value.put("createdAt", timestamp);
        return StreamRecords
                .mapBacked(value)
                .withId(RecordId.of(recordId))
                .withStreamKey(roomId);
    }
}
