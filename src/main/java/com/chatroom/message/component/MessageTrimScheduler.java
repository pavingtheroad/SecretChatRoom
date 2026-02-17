package com.chatroom.message.component;

import com.chatroom.message.service.MessageTrimService;
import com.chatroom.room.dao.RoomCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@EnableScheduling
public class MessageTrimScheduler {
    private static final Logger log = LoggerFactory.getLogger(MessageTrimScheduler.class);
    private final RoomCacheRepository roomCacheRepository;
    private final MessageTrimService messageTrimService;
    public MessageTrimScheduler(RoomCacheRepository roomCacheRepository,
                                MessageTrimService messageTrimService) {
        this.roomCacheRepository = roomCacheRepository;
        this.messageTrimService = messageTrimService;
    }

    @Scheduled(fixedDelay = 300000)    // 300000ms = 5min
    public void trimRooms(){
        Set<String> roomIds = roomCacheRepository.getAllRoomIds();

        for (String roomId : roomIds){
            try{
                messageTrimService.trimRoomMessages(roomId);
            } catch (Exception e){
                log.warn("Failed to Trim Message for Room:{}", roomId, e);
            }
        }
    }
}
