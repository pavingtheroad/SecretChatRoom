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
    private final RoomStateRepository roomStateRepository;
    public MessageWriteService(MessageCacheRepository messageCacheRepository, RoomStateRepository roomStateRepository){
        this.messageCacheRepository = messageCacheRepository;
        this.roomStateRepository = roomStateRepository;
    }
    public ChatMessage saveMessage(String senderId, String roomId, String content){
        ChatMessage message = new ChatMessage(
                roomId,
                senderId,
                MessageType.TEXT,
                content,
                Instant.now().toEpochMilli()
        );
        RecordId id = messageCacheRepository.saveMessage(message);
        roomStateRepository.updateLastMessageId(roomId, id.getValue().toString());
        return message;
    }
}