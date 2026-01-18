package com.chatroom.message.dto;

import lombok.Data;

@Data
public class MessageDTO {
    // messageId 由Redist Stream自动生成
    private String roomId;
    private String userPKId;
    private String type;
    private String content;    // JSON 消息具体内容
    private long timestamp;    // 消息时间戳
}