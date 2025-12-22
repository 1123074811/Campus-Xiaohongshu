package com.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.springboot.entity.AiAssistantConfig;
import com.example.springboot.mapper.AiAssistantConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConfigService {
    
    private final AiAssistantConfigMapper configMapper;
    
    // 配置缓存，避免频繁查询数据库
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    
    // 默认配置值
    private static final Map<String, String> DEFAULT_CONFIG = Map.of(
        "systemPrompt", "你是一个热心、可爱的智能助手，叫哈基聪，请以哈基聪的语气回答问题",
        "maxMessages", "20",
        "sessionExpireHours", "24",
        "enableThinking", "true",
        "modelName", "deepseek-r1:7b",
        "apiUrl", "http://localhost:11434"
    );
    
    /**
     * 获取配置值
     */
    public String getConfig(String key) {
        // 先从缓存获取
        String value = configCache.get(key);
        if (value != null) {
            return value;
        }
        
        // 从数据库获取
        try {
            LambdaQueryWrapper<AiAssistantConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiAssistantConfig::getConfigKey, key);
            AiAssistantConfig config = configMapper.selectOne(wrapper);
            
            if (config != null && config.getConfigValue() != null) {
                value = config.getConfigValue();
                configCache.put(key, value);
                return value;
            }
        } catch (Exception e) {
            log.warn("获取配置失败: key={}, error={}", key, e.getMessage());
        }
        
        // 返回默认值
        value = DEFAULT_CONFIG.get(key);
        if (value != null) {
            configCache.put(key, value);
        }
        return value;
    }
    
    /**
     * 获取配置值（带类型转换）
     */
    public int getIntConfig(String key) {
        String value = getConfig(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值转换为整数失败: key={}, value={}", key, value);
            String defaultValue = DEFAULT_CONFIG.get(key);
            return defaultValue != null ? Integer.parseInt(defaultValue) : 0;
        }
    }
    
    /**
     * 获取配置值（布尔类型）
     */
    public boolean getBooleanConfig(String key) {
        String value = getConfig(key);
        return Boolean.parseBoolean(value);
    }
    
    /**
     * 获取系统提示词
     */
    public String getSystemPrompt() {
        return getConfig("systemPrompt");
    }
    
    /**
     * 获取最大消息数
     */
    public int getMaxMessages() {
        return getIntConfig("maxMessages");
    }
    
    /**
     * 获取会话过期时间（小时）
     */
    public int getSessionExpireHours() {
        return getIntConfig("sessionExpireHours");
    }
    
    /**
     * 是否启用思考显示
     */
    public boolean isEnableThinking() {
        return getBooleanConfig("enableThinking");
    }
    
    /**
     * 获取所有配置
     */
    public Map<String, String> getAllConfigs() {
        Map<String, String> allConfigs = new HashMap<>();
        
        try {
            List<AiAssistantConfig> configs = configMapper.selectList(null);
            for (AiAssistantConfig config : configs) {
                allConfigs.put(config.getConfigKey(), config.getConfigValue());
                // 更新缓存
                configCache.put(config.getConfigKey(), config.getConfigValue());
            }
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
        }
        
        // 添加默认配置（如果数据库中没有）
        for (Map.Entry<String, String> entry : DEFAULT_CONFIG.entrySet()) {
            allConfigs.putIfAbsent(entry.getKey(), entry.getValue());
        }
        
        return allConfigs;
    }
    
    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        log.info("刷新AI配置缓存");
        configCache.clear();
        // 预加载常用配置
        getSystemPrompt();
        getMaxMessages();
        getSessionExpireHours();
        isEnableThinking();
    }
    
    /**
     * 清除指定配置的缓存
     */
    public void clearCache(String key) {
        configCache.remove(key);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        configCache.clear();
    }
}