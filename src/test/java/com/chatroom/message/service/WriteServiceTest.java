package com.chatroom.message.service;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WriteServiceTest {
    @Mock
    private MessageCacheRepository messageCacheRepository;

    @InjectMocks
    private MessageWriteService messageWriteService;

    @Test
    void saveMessage_shouldReturnNull_whenRepositoryReturnsNull() {
        // given
        when(messageCacheRepository.saveMessage(any(), eq("req-1")))
                .thenReturn(null);

        // when
        ChatMessage result = messageWriteService
                .saveMessage("user1", "room1", "hello", "req-1");

        // then
        assertNull(result);
    }

    @Test
    void saveMessage_shouldReturnMessage_whenRepositoryReturnsId() {
        // given
        when(messageCacheRepository.saveMessage(any(), eq("req-1")))
                .thenReturn(100L);

        // when
        ChatMessage result = messageWriteService
                .saveMessage("user1", "room1", "hello", "req-1");

        // then
        assertNotNull(result);
        assertEquals("100", result.streamId());
        assertEquals("room1", result.roomId());
        assertEquals("user1", result.senderId());
        assertEquals("hello", result.content());
        assertNotNull(result.createdAt());
    }
}
