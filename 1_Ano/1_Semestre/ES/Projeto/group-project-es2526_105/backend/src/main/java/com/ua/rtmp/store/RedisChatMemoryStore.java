package com.ua.rtmp.store;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;

/**
 * Redis-based implementation of ChatMemoryStore using Jedis.
 * Stores chat messages as JSON in Redis with TTL of 24 hours.
 */
@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {

    private static final String KEY_PREFIX = "langchain4j:chat:";
    private static final int TTL_SECONDS = 86400; // 24 hours

    private final JedisPooled jedis;

    public RedisChatMemoryStore(JedisPooled jedis) {
        this.jedis = jedis;
        log.info("RedisChatMemoryStore initialized");
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = buildKey(memoryId);
        try {
            String json = jedis.get(key);
            if (json == null || json.isEmpty()) {
                log.debug("No messages found for memoryId: {}", memoryId);
                return new ArrayList<>();
            }
            List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
            log.debug("Retrieved {} messages for memoryId: {}", messages.size(), memoryId);
            return messages;
        } catch (Exception e) {
            log.error("Error retrieving messages for memoryId: {}", memoryId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = buildKey(memoryId);
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            jedis.setex(key, TTL_SECONDS, json);
            log.debug("Updated {} messages for memoryId: {}", messages.size(), memoryId);
        } catch (Exception e) {
            log.error("Error updating messages for memoryId: {}", memoryId, e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = buildKey(memoryId);
        try {
            jedis.del(key);
            log.debug("Deleted messages for memoryId: {}", memoryId);
        } catch (Exception e) {
            log.error("Error deleting messages for memoryId: {}", memoryId, e);
        }
    }

    private String buildKey(Object memoryId) {
        return KEY_PREFIX + memoryId.toString();
    }
}
