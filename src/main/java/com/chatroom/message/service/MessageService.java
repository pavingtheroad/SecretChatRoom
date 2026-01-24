// 消息生命周期管理
package com.chatroom.message.service;

import com.chatroom.message.dto.MessageDTO;
import com.chatroom.message.dao.MessageCacheRepository;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MessageService {
    private final MessageCacheRepository messageCacheRepository;
    public MessageService(MessageCacheRepository messageCacheRepository){
        this.messageCacheRepository = messageCacheRepository;
    }



    /**
     * 保存并广播消息
     * @param message 消息对象
     * @param roomId 房间ID
     */
    public void saveAndBroadcast(MessageDTO message, String roomId){

    }

    /**
     * 剪枝（根据房间TTL设置进行剪枝）
     * @Scheduled周期性任务
     */
    @Scheduled(fixedDelay = 60000)    // 周期60秒
    public void pruneExpiredMessages(){
        /*
        get all roomIds
        for each roomId :
            get roomTTL
            cutoffId = (System.currentTimeMillis() - ttlMillis) + "-0";
            XTRIM MINID=cutoffId
         */
    }


    // Stream id比较 <ms>-<seq>
    private boolean isLess(String id1, String id2){
        return compare(id1, id2) < 0;
    }
    private int compare(String id1, String id2){
        String[] id1s = id1.split("-");
        String[] id2s = id2.split("-");
        int t = Long.compare(Long.parseLong(id1s[0]), Long.parseLong(id2s[0]));    // (x < y) ? -1 : ((x == y) ? 0 : 1);
        if (t != 0){
            return t;
        }
        return Long.compare(Long.parseLong(id1s[1]), Long.parseLong(id2s[1]));
    }

}