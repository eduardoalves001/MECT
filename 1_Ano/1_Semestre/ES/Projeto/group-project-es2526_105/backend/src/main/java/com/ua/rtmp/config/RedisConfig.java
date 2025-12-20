package com.ua.rtmp.config;

import com.ua.rtmp.store.RedisChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

@Slf4j
@Configuration
public class RedisConfig {

    @Value("${redis.host:redis}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Bean
    public JedisPooled jedisPooled() {
        log.info("Initializing Redis connection: host={}, port={}", redisHost, redisPort);
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore(JedisPooled jedisPooled) {
        log.info("Creating RedisChatMemoryStore for LangChain4j");
        return new RedisChatMemoryStore(jedisPooled);
    }
}
