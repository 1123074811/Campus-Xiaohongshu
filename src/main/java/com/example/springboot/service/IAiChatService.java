package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.AiChatMessage;
import com.example.springboot.entity.AiChatSession;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI聊天服务接口
 */
public interface IAiChatService extends IService<AiChatMessage> {
    
    /**
     * 创建新的聊天会话
     */
    AiChatSession createSession(Long userId, String title);
    
    /**
     * 获取用户的聊天会话列表
     */
    List<AiChatSession> getUserSessions(Long userId);
    
    /**
     * 获取会话的聊天记录
     */
    List<AiChatMessage> getSessionMessages(String sessionId, Long userId);
    
    /**
     * 发送消息并获取AI回复流
     */
    Flux<String> chatStream(String sessionId, Long userId, String message);
    
    /**
     * 删除会话
     */
    boolean deleteSession(String sessionId, Long userId);
    
    /**
     * 更新会话标题
     */
    boolean updateSessionTitle(String sessionId, Long userId, String title);
}