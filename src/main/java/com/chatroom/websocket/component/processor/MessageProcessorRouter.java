package com.chatroom.websocket.component.processor;

import com.chatroom.websocket.enums.MessageType;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MessageProcessorRouter {
    private final Map<MessageType, MessageProcessor> processorMap;

    public MessageProcessorRouter(List<MessageProcessor> processors){
        this.processorMap = new HashMap<>();
        for (MessageProcessor processor : processors) {
            processorMap.put(processor.supportType(), processor);
        }
    }
    public MessageProcessor getProcessor(MessageType type){
        return processorMap.get(type);
    }

}
