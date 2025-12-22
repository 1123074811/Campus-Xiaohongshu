package com.example.springboot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis聊天记忆存储库
 */
@Component
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String CHAT_MEMORY_PREFIX = "ai:chat:memory:";
    private static final long EXPIRE_TIME = 24 * 60 * 60; // 24小时过期
    
    public RedisChatMemoryRepository(@Qualifier("aiStringRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        try {
            String key = CHAT_MEMORY_PREFIX + conversationId;
            // 将消息转换为简单的字符串格式存储
            List<SimpleMessage> simpleMessages = messages.stream()
                    .map(this::convertToSimpleMessage)
                    .toList();
            String jsonValue = objectMapper.writeValueAsString(simpleMessages);
            redisTemplate.opsForValue().set(key, jsonValue, EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save chat memory", e);
        }
    }
    
    @Override
    public List<Message> findByConversationId(String conversationId) {
        try {
            String key = CHAT_MEMORY_PREFIX + conversationId;
            String jsonValue = redisTemplate.opsForValue().get(key);
            if (jsonValue == null) {
                return List.of();
            }
            
            CollectionType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, SimpleMessage.class);
            List<SimpleMessage> simpleMessages = objectMapper.readValue(jsonValue, listType);
            
            return simpleMessages.stream()
                    .map(this::convertToMessage)
                    .toList();
        } catch (Exception e) {
            // 如果反序列化失败，返回空列表而不是抛出异常
            return List.of();
        }
    }
    
    @Override
    public void deleteByConversationId(String conversationId) {
        String key = CHAT_MEMORY_PREFIX + conversationId;
        redisTemplate.delete(key);
    }
    
    @Override
    public List<String> findConversationIds() {
        // 获取所有会话ID
        String pattern = CHAT_MEMORY_PREFIX + "*";
        return redisTemplate.keys(pattern).stream()
                .map(key -> key.toString().replace(CHAT_MEMORY_PREFIX, ""))
                .toList();
    }
    
    /**
     * 将Spring AI的Message转换为简单的消息对象
     */
    private SimpleMessage convertToSimpleMessage(Message message) {
        SimpleMessage simple = new SimpleMessage();
        simple.setContent(message.getText());
        simple.setMessageType(message.getMessageType().name());
        return simple;
    }
    
    /**
     * 将简单的消息对象转换为Spring AI的Message
     */
    private Message convertToMessage(SimpleMessage simple) {
        return switch (simple.getMessageType()) {
            case "USER" -> new org.springframework.ai.chat.messages.UserMessage(simple.getContent());
            case "ASSISTANT" -> new org.springframework.ai.chat.messages.AssistantMessage(simple.getContent());
            case "SYSTEM" -> new org.springframework.ai.chat.messages.SystemMessage(simple.getContent());
            default -> new org.springframework.ai.chat.messages.UserMessage(simple.getContent());
        };
    }
    
    /**
     * 简单的消息对象，用于序列化
     */
    public static class SimpleMessage {
        private String content;
        private String messageType;
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
        
        public String getMessageType() {
            return messageType;
        }
        
        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }
}