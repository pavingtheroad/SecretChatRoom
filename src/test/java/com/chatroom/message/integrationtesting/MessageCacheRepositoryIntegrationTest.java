package com.chatroom.message.integrationtesting;

import com.chatroom.message.dao.MessageCacheRepository;
import com.chatroom.message.domain.MessageType;
import com.chatroom.message.entity.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.AutoConfigureDataRedis;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers    // 自动启动、清除容器
@AutoConfigureDataRedis    // 自动配置Redis相关Bean RedisTemplate、StringRedisTemplate 等
public class MessageCacheRepositoryIntegrationTest {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    @DynamicPropertySource
    static void overrideRedisProperties(DynamicPropertyRegistry registry){
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port",
                () -> redis.getMappedPort(6379));
    }
    @Autowired
    private MessageCacheRepository messageCacheRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @BeforeEach
    void clean(){
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }
    @Test
    void shouldSaveMessageAndReturnId_saveMessage(){
        ChatMessage msg = new ChatMessage(
                null,
                "room1",
                "user1",
                MessageType.TEXT,
                "hello",
                System.currentTimeMillis()
        );
        String result = messageCacheRepository.saveMessage(msg, "req-1");

        assertThat(result).isNotNull();

        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.rangeMessages(
                        "room1",
                        Range.unbounded(),
                        Limit.limit().count(1)
                );
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("content"))
                .isEqualTo("hello");
    }
    @Test
    void shouldNotSaveSameMessageTwice_saveMessage(){
        ChatMessage msg = new ChatMessage(
                null,
                "room1",
                "user1",
                MessageType.TEXT,
                "hello",
                System.currentTimeMillis()
        );
        String first = messageCacheRepository.saveMessage(msg, "req-1");
        String second = messageCacheRepository.saveMessage(msg, "req-1");    // 重复请求
        assertThat(first).isNotNull();
        assertThat(second).isNull();
        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.rangeMessages(
                        "room1",
                        Range.unbounded(),
                        Limit.limit().count(10)
                );

        assertThat(records).hasSize(1);
    }
    @Test
    void shouldReturnMessagesInReverseOrder(){
        for (int i = 0; i < 3; i++){
            ChatMessage msg = new ChatMessage(
                    null,
                    "room1",
                    "user" + i,
                    MessageType.TEXT,
                    "hello",
                    System.currentTimeMillis()
            );
            messageCacheRepository.saveMessage(msg, "req-" + i);
        }
        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.reverseRangeMessages(
                        "room1",
                        Range.unbounded(),
                        Limit.limit().count(10)
                );
        assertThat(records).hasSize(3);
        assertThat(records.get(0).getValue().get("senderId")).isEqualTo("user2");
    }
    @Test
    void shouldReturnMessagesInOrder(){
        for (int i = 0; i < 3; i++){
            ChatMessage msg = new ChatMessage(
                    null,
                    "room1",
                    "user" + i,
                    MessageType.TEXT,
                    "hello",
                    System.currentTimeMillis()
            );
            messageCacheRepository.saveMessage(msg, "req-" + i);
        }
        List<MapRecord<String, Object, Object>> records =
                messageCacheRepository.rangeMessages(
                        "room1",
                        Range.unbounded(),
                        Limit.limit().count(10)
                );
        assertThat(records).hasSize(3);
        assertThat(records.get(0).getValue().get("senderId")).isEqualTo("user0");
    }
    @Test
    void shouldTrimOldMessages(){
        for (int i = 0; i < 10; i++){
            ChatMessage msg = new ChatMessage(
                    null,
                    "room1",
                    "user" + i,
                    MessageType.TEXT,
                    "hello",
                    System.currentTimeMillis()
            );
            messageCacheRepository.saveMessage(msg, "req-" + i);
        }

        String cutoffId = messageCacheRepository.getLastMessageId("room1").orElseThrow();

        messageCacheRepository.trimMessage("room1", cutoffId);

        for (int i = 0; i < 3; i++){
            ChatMessage msg = new ChatMessage(
                    null,
                    "room1",
                    "user" + i,
                    MessageType.TEXT,
                    "hello",
                    System.currentTimeMillis()
            );
            messageCacheRepository.saveMessage(msg, "req-" + i);
        }
    }
}
