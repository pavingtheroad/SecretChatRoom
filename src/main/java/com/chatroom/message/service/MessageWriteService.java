// 消息生命周期管理
package com.chatroom.message.service;

import com.chatroom.message.dao.RoomStateRepository;
import com.chatroom.message.domain.MessageType;
import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.entity.ChatMessage;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MessageWriteService {
    private final MessageCacheRepository messageCacheRepository;
    public MessageWriteService(MessageCacheRepository messageCacheRepository){
        this.messageCacheRepository = messageCacheRepository;
    }
    public ChatMessage saveMessage(String senderId, String roomId, String content, String requestId){
        ChatMessage message = new ChatMessage(
                null,
                roomId,
                senderId,
                MessageType.TEXT,
                content,
                Instant.now().toEpochMilli()
        );
        Long messageId = messageCacheRepository.saveMessage(message, requestId);
        if (messageId == null){
            return null;
        }
        return new ChatMessage(
                messageId.toString(),
                roomId,
                senderId,
                MessageType.TEXT,
                content,
                Instant.now().toEpochMilli()
        );
    }
}