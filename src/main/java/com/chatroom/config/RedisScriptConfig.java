package com.chatroom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

@Configuration
public class RedisScriptConfig {
    @Bean
    public RedisScript<Long> deleteRoomScript(){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/delete_room.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
    @Bean
    public RedisScript<Long> addUserToRoomScript(){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/add_user_to_room.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
    @Bean
    public RedisScript<Long> removeUserFromRoomScript(){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/remove_user_from_room.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }
    @Bean
    public RedisScript<Long> createRoomScript(){
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/create_room.lua"));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public RedisScript<List> batchGetRoomInfoScript(){
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/batch_get_room_info.lua"));
        redisScript.setResultType(List.class);
        return redisScript;
    }
}
