package com.chatroom.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.chatroom.**.dao")
public class MybatisConfig {
}
