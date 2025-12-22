package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.AiChatMessage;
import com.example.springboot.entity.AiChatSession;
import com.example.springboot.service.IAiChatService;
import com.example.springboot.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI助手控制器
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {
    
    private final IAiChatService aiChatService;
    
    /**
     * 创建新的聊天会话
     */
    @PostMapping("/session")
    public Result createSession(@RequestBody Map<String, String> params,
                                               @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || currentUser.getId() == null) {
                return Result.error("401", "用户未登录或登录已过期");
            }
            
            Long userId = currentUser.getId().longValue();
            String title = params.get("title");
            
            log.info("创建AI会话请求: userId={}, title={}", userId, title);
            
            AiChatSession session = aiChatService.createSession(userId, title);
            
            log.info("AI会话创建成功: sessionId={}", session.getSessionId());
            return Result.success(session);
        } catch (Exception e) {
            log.error("创建AI会话失败: ", e);
            return Result.error("500", "创建会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取用户的聊天会话列表
     */
    @GetMapping("/sessions")
    public Result getUserSessions(@RequestHeader("token") String token) {
        Long userId = TokenUtils.getCurrentUser().getId().longValue();
        List<AiChatSession> sessions = aiChatService.getUserSessions(userId);
        return Result.success(sessions);
    }
    
    /**
     * 获取会话的聊天记录
     */
    @GetMapping("/session/{sessionId}/messages")
    public Result getSessionMessages(@PathVariable String sessionId,
                                                           @RequestHeader("token") String token) {
        Long userId = TokenUtils.getCurrentUser().getId().longValue();
        List<AiChatMessage> messages = aiChatService.getSessionMessages(sessionId, userId);
        return Result.success(messages);
    }
    
    /**
     * 发送消息并获取AI回复流
     */
    @PostMapping(value = "/chat", produces = "text/plain;charset=UTF-8")
    public Flux<String> chat(@RequestParam String sessionId,
                              @RequestParam String message,
                              @RequestHeader("token") String token) {
        Long userId = TokenUtils.getCurrentUser().getId().longValue();
        
        return aiChatService.chatStream(sessionId, userId, message)
                .doOnError(error -> {
                    log.error("AI聊天出错: ", error);
                })
                .onErrorReturn("抱歉，我遇到了一些问题，请稍后再试。");
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/session/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId,
                                         @RequestHeader("token") String token) {
        Long userId = TokenUtils.getCurrentUser().getId().longValue();
        boolean success = aiChatService.deleteSession(sessionId, userId);
        return Result.success(success);
    }
    
    /**
     * 更新会话标题
     */
    @PutMapping("/session/{sessionId}/title")
    public Result updateSessionTitle(@PathVariable String sessionId,
                                              @RequestBody Map<String, String> params,
                                              @RequestHeader("token") String token) {
        Long userId = TokenUtils.getCurrentUser().getId().longValue();
        String title = params.get("title");
        
        boolean success = aiChatService.updateSessionTitle(sessionId, userId, title);
        return Result.success(success);
    }
    
    /**
     * 获取欢迎消息
     */
    @GetMapping("/welcome")
    public Result getWelcomeMessage() {
        return Result.success("你好！我是哈基聪，你的智能助手。有什么可以帮助你的吗？");
    }
}