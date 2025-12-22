package com.example.springboot.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.AiChatMessage;
import com.example.springboot.entity.AiChatSession;
import com.example.springboot.mapper.AiChatMessageMapper;
import com.example.springboot.mapper.AiChatSessionMapper;
import com.example.springboot.service.IAiChatService;
import com.example.springboot.service.DynamicChatClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

/**
 * AI聊天服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl extends ServiceImpl<AiChatMessageMapper, AiChatMessage> implements IAiChatService {
    
    private final AiChatSessionMapper sessionMapper;
    private final DynamicChatClientService dynamicChatClientService;
    
    @Override
    @Transactional
    public AiChatSession createSession(Long userId, String title) {
        // 验证输入参数
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        AiChatSession session = new AiChatSession();
        String sessionId = IdUtil.fastSimpleUUID();
        
        // 确保sessionId不为空
        if (StrUtil.isBlank(sessionId)) {
            throw new RuntimeException("生成会话ID失败");
        }
        
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setTitle(StrUtil.isBlank(title) ? "新对话" : title);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        session.setIsDeleted(0); // 显式设置删除标志
        
        log.info("创建AI会话: sessionId={}, userId={}, title={}", sessionId, userId, session.getTitle());
        
        try {
            sessionMapper.insert(session);
            log.info("AI会话创建成功: id={}", session.getId());
        } catch (Exception e) {
            log.error("创建AI会话失败: ", e);
            throw new RuntimeException("创建会话失败: " + e.getMessage());
        }
        
        return session;
    }
    
    @Override
    public List<AiChatSession> getUserSessions(Long userId) {
        LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatSession::getUserId, userId)
                .orderByDesc(AiChatSession::getUpdateTime);
        return sessionMapper.selectList(wrapper);
    }
    
    @Override
    public List<AiChatMessage> getSessionMessages(String sessionId, Long userId) {
        LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getUserId, userId)
                .orderByAsc(AiChatMessage::getCreateTime);
        return this.list(wrapper);
    }
    
    @Override
    @Transactional
    public Flux<String> chatStream(String sessionId, Long userId, String message) {
        // 保存用户消息
        AiChatMessage userMessage = new AiChatMessage();
        userMessage.setSessionId(sessionId);
        userMessage.setUserId(userId);
        userMessage.setMessageType(AiChatMessage.MessageType.USER.getCode());
        userMessage.setContent(message);
        userMessage.setCreateTime(LocalDateTime.now());
        this.save(userMessage);
        
        // 更新会话时间
        updateSessionTime(sessionId);
        
        // 用于收集完整的AI回复
        StringBuilder responseBuilder = new StringBuilder();
        
        // 构建包含思考指令的提示词
        String enhancedPrompt = buildEnhancedPrompt(message);
        
        // 获取动态配置的ChatClient
        ChatClient chatClient = dynamicChatClientService.getCurrentChatClient();
        
        // 获取AI回复流
        return chatClient.prompt()
                .user(enhancedPrompt)
                .advisors(a -> a.param(CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .map(this::processAiChunk) // 处理AI回复块，添加思考标识
                .doOnNext(chunk -> {
                    // 收集每个流式块（处理后的）
                    responseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 流结束后保存完整的AI回复
                    String completeResponse = responseBuilder.toString();
                    if (!completeResponse.isEmpty()) {
                        saveAiResponse(sessionId, userId, completeResponse);
                    }
                })
                .doOnError(error -> {
                    log.error("AI聊天出错: ", error);
                    // 出错时也保存错误信息
                    saveAiResponse(sessionId, userId, "抱歉，我遇到了一些问题，请稍后再试。");
                });
    }
    
    @Override
    @Transactional
    public boolean deleteSession(String sessionId, Long userId) {
        // 删除会话
        LambdaQueryWrapper<AiChatSession> sessionWrapper = new LambdaQueryWrapper<>();
        sessionWrapper.eq(AiChatSession::getSessionId, sessionId)
                .eq(AiChatSession::getUserId, userId);
        int sessionResult = sessionMapper.delete(sessionWrapper);
        
        // 删除消息
        LambdaQueryWrapper<AiChatMessage> messageWrapper = new LambdaQueryWrapper<>();
        messageWrapper.eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getUserId, userId);
        boolean messageResult = this.remove(messageWrapper);
        
        return sessionResult > 0;
    }
    
    @Override
    @Transactional
    public boolean updateSessionTitle(String sessionId, Long userId, String title) {
        LambdaUpdateWrapper<AiChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiChatSession::getSessionId, sessionId)
                .eq(AiChatSession::getUserId, userId)
                .set(AiChatSession::getTitle, title)
                .set(AiChatSession::getUpdateTime, LocalDateTime.now());
        
        return sessionMapper.update(null, wrapper) > 0;
    }
    
    /**
     * 构建增强的提示词，指导AI使用思考标识
     */
    private String buildEnhancedPrompt(String userMessage) {
        return """
                请在回答问题时，将你的思考过程用特殊标识包围。
                
                使用格式：
                <thinking>
                这里是你的思考过程，包括分析、推理、考虑等内容
                </thinking>
                
                然后给出你的最终回答。
                
                用户问题：%s
                """.formatted(userMessage);
    }
    
    /**
     * 处理AI回复块，为思考内容添加特殊标识
     */
    private String processAiChunk(String chunk) {
        // 这里可以进一步处理思考标识，比如转换为前端识别的格式
        return chunk;
    }
    
    /**
     * 更新会话时间
     */
    private void updateSessionTime(String sessionId) {
        LambdaUpdateWrapper<AiChatSession> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiChatSession::getSessionId, sessionId)
                .set(AiChatSession::getUpdateTime, LocalDateTime.now());
        
        sessionMapper.update(null, wrapper);
    }
    
    /**
     * 保存AI回复消息
     */
    public void saveAiResponse(String sessionId, Long userId, String response) {
        AiChatMessage aiMessage = new AiChatMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setUserId(userId);
        aiMessage.setMessageType(AiChatMessage.MessageType.ASSISTANT.getCode());
        aiMessage.setContent(response);
        aiMessage.setCreateTime(LocalDateTime.now());
        this.save(aiMessage);
    }
}