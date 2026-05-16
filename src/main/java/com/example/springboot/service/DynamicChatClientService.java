package com.example.springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态ChatClient服务
 * 根据当前配置动态创建和管理ChatClient实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicChatClientService {
    
    private final ObjectProvider<OllamaChatModel> modelProvider;
    private final ChatMemoryRepository chatMemoryRepository;
    private final AiConfigService aiConfigService;
    
    // ChatClient缓存，按配置版本缓存
    private final ConcurrentHashMap<String, ChatClient> clientCache = new ConcurrentHashMap<>();
    private volatile String currentConfigVersion = null;
    
    /**
     * 获取当前配置的ChatClient
     */
    public ChatClient getCurrentChatClient() {
        String configVersion = generateConfigVersion();
        
        // 如果配置版本发生变化，清除缓存
        if (!configVersion.equals(currentConfigVersion)) {
            log.info("AI配置发生变化，清除ChatClient缓存: {} -> {}", currentConfigVersion, configVersion);
            clientCache.clear();
            currentConfigVersion = configVersion;
        }
        
        // 从缓存获取或创建新的ChatClient
        return clientCache.computeIfAbsent(configVersion, k -> createChatClient());
    }
    
    /**
     * 创建新的ChatClient实例
     */
    private ChatClient createChatClient() {
        try {
            OllamaChatModel model = modelProvider.getIfAvailable();
            if (model == null) {
                throw new IllegalStateException("AI助手暂不可用：未检测到 Ollama 模型配置，请确认 Ollama 相关依赖与配置是否启用");
            }
            
            // 获取当前配置
            String systemPrompt = aiConfigService.getSystemPrompt();
            int maxMessages = aiConfigService.getMaxMessages();
            
            log.info("创建新的ChatClient: systemPrompt={}, maxMessages={}", 
                    systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "...", maxMessages);
            
            // 创建动态配置的ChatMemory
            ChatMemory chatMemory = MessageWindowChatMemory.builder()
                    .chatMemoryRepository(chatMemoryRepository)
                    .maxMessages(maxMessages)
                    .build();
            
            // 创建ChatClient
            return ChatClient.builder(model)
                    .defaultSystem(systemPrompt)
                    .defaultAdvisors(
                            new SimpleLoggerAdvisor(),
                            MessageChatMemoryAdvisor.builder(chatMemory).build()
                    )
                    .build();
        } catch (Exception e) {
            log.error("创建ChatClient失败", e);
            throw new RuntimeException("创建ChatClient失败", e);
        }
    }
    
    /**
     * 生成配置版本标识
     * 基于关键配置项生成版本号，用于判断配置是否发生变化
     */
    private String generateConfigVersion() {
        String systemPrompt = aiConfigService.getSystemPrompt();
        int maxMessages = aiConfigService.getMaxMessages();
        
        // 使用配置内容的哈希值作为版本标识
        return String.valueOf((systemPrompt + maxMessages).hashCode());
    }
    
    /**
     * 强制刷新ChatClient
     * 清除所有缓存，下次获取时会重新创建
     */
    public void refreshChatClient() {
        log.info("强制刷新ChatClient");
        clientCache.clear();
        currentConfigVersion = null;
        aiConfigService.refreshCache();
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        return String.format("ChatClient缓存: 当前版本=%s, 缓存大小=%d", 
                currentConfigVersion, clientCache.size());
    }
}