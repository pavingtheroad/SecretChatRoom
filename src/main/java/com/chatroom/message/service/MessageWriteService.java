// 消息生命周期管理
package com.chatroom.message.service;

import com.chatroom.message.dao.RoomStateRepository;
import com.chatroom.message.domain.MessageType;
import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.entity.Message;
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
    public void sendMessage(Long roomId, String content){
        Long senderId = 1001L;    // 实际由校验服务获取当前用户ID
        RecordId id = messageCacheRepository.saveMessage(new Message(
                roomId,
                senderId,
                MessageType.TEXT,
                content,
                Instant.now().toEpochMilli()
        ));
        roomStateRepository.updateLastMessageId(roomId.toString(), id.getValue().toString());
    }
}