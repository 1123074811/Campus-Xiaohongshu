package com.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.AiChatMessage;
import com.example.springboot.entity.AiChatSession;
import com.example.springboot.entity.Account;
import com.example.springboot.entity.User;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.AiAssistantConfig;
import com.example.springboot.mapper.AiChatMessageMapper;
import com.example.springboot.mapper.AiChatSessionMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.AiAssistantConfigMapper;
import com.example.springboot.service.DynamicChatClientService;
import com.example.springboot.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台AI助手管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAiAssistantController {
    
    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final AiAssistantConfigMapper configMapper;
    private final DynamicChatClientService dynamicChatClientService;
    
    /**
     * 根据用户ID获取用户信息（支持User和Admin）
     */
    private Account getUserById(Long userId) {
        // 先尝试从用户表查询
        User user = userMapper.selectById(userId);
        if (user != null) {
            return user;
        }
        
        // 再尝试从管理员表查询
        Admin admin = adminMapper.selectById(userId);
        return admin;
    }
    
    /**
     * 获取AI助手统计数据
     */
    @GetMapping("/stats")
    public Result getStats(@RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            Map<String, Object> stats = new HashMap<>();
            
            // 总会话数
            Long totalSessions = sessionMapper.selectCount(null);
            stats.put("totalSessions", totalSessions);
            
            // 总消息数
            Long totalMessages = messageMapper.selectCount(null);
            stats.put("totalMessages", totalMessages);
            
            // 活跃用户数（有会话的用户）
            LambdaQueryWrapper<AiChatSession> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.select(AiChatSession::getUserId).groupBy(AiChatSession::getUserId);
            List<AiChatSession> uniqueUserSessions = sessionMapper.selectList(sessionWrapper);
            stats.put("activeUsers", uniqueUserSessions.size());
            
            // 今日会话数
            LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            LambdaQueryWrapper<AiChatSession> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.between(AiChatSession::getCreateTime, todayStart, todayEnd);
            Long todaySessions = sessionMapper.selectCount(todayWrapper);
            stats.put("todaySessions", todaySessions);
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取AI助手统计数据失败: ", e);
            return Result.error("500", "获取统计数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询AI会话列表
     */
    @GetMapping("/sessions")
    public Result getSessions(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "20") Integer size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            Page<AiChatSession> pageObj = new Page<>(page, size);
            LambdaQueryWrapper<AiChatSession> wrapper = new LambdaQueryWrapper<>();
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.like(AiChatSession::getTitle, keyword.trim());
            }
            
            // 日期范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(AiChatSession::getCreateTime, startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(AiChatSession::getCreateTime, endDate + " 23:59:59");
            }
            
            wrapper.orderByDesc(AiChatSession::getUpdateTime);
            
            IPage<AiChatSession> result = sessionMapper.selectPage(pageObj, wrapper);
            
            // 补充用户信息和消息数量
            List<AiChatSession> sessions = result.getRecords();
            for (AiChatSession session : sessions) {
                // 获取用户信息
                Account user = getUserById(session.getUserId());
                if (user != null) {
                    session.setUserNickname(user.getNickname());
                }
                
                // 获取消息数量
                LambdaQueryWrapper<AiChatMessage> messageWrapper = new LambdaQueryWrapper<>();
                messageWrapper.eq(AiChatMessage::getSessionId, session.getSessionId());
                Long messageCount = messageMapper.selectCount(messageWrapper);
                session.setMessageCount(messageCount.intValue());
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取AI会话列表失败: ", e);
            return Result.error("500", "获取会话列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询AI消息列表
     */
    @GetMapping("/messages")
    public Result getMessages(@RequestParam(defaultValue = "1") Integer page,
                             @RequestParam(defaultValue = "20") Integer size,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String messageType,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            Page<AiChatMessage> pageObj = new Page<>(page, size);
            LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
            
            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                wrapper.like(AiChatMessage::getContent, keyword.trim());
            }
            
            // 消息类型筛选
            if (messageType != null && !messageType.isEmpty()) {
                wrapper.eq(AiChatMessage::getMessageType, messageType);
            }
            
            // 日期范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                wrapper.ge(AiChatMessage::getCreateTime, startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                wrapper.le(AiChatMessage::getCreateTime, endDate + " 23:59:59");
            }
            
            wrapper.orderByDesc(AiChatMessage::getCreateTime);
            
            IPage<AiChatMessage> result = messageMapper.selectPage(pageObj, wrapper);
            
            // 补充用户信息
            List<AiChatMessage> messages = result.getRecords();
            for (AiChatMessage message : messages) {
                Account user = getUserById(message.getUserId());
                if (user != null) {
                    message.setUserNickname(user.getNickname());
                }
            }
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取AI消息列表失败: ", e);
            return Result.error("500", "获取消息列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定会话的消息记录
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result getSessionMessages(@PathVariable String sessionId,
                                   @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            LambdaQueryWrapper<AiChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiChatMessage::getSessionId, sessionId)
                   .orderByAsc(AiChatMessage::getCreateTime);
            
            List<AiChatMessage> messages = messageMapper.selectList(wrapper);
            
            // 补充用户信息
            for (AiChatMessage message : messages) {
                Account user = getUserById(message.getUserId());
                if (user != null) {
                    message.setUserNickname(user.getNickname());
                }
            }
            
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取会话消息失败: ", e);
            return Result.error("500", "获取会话消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除指定会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result deleteSession(@PathVariable String sessionId,
                               @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            // 删除会话
            LambdaQueryWrapper<AiChatSession> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.eq(AiChatSession::getSessionId, sessionId);
            int sessionResult = sessionMapper.delete(sessionWrapper);
            
            // 删除相关消息
            LambdaQueryWrapper<AiChatMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper.eq(AiChatMessage::getSessionId, sessionId);
            messageMapper.delete(messageWrapper);
            
            return Result.success(sessionResult > 0);
        } catch (Exception e) {
            log.error("删除会话失败: ", e);
            return Result.error("500", "删除会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除会话
     */
    @DeleteMapping("/sessions/batch")
    public Result batchDeleteSessions(@RequestBody Map<String, List<String>> request,
                                     @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            List<String> sessionIds = request.get("sessionIds");
            if (sessionIds == null || sessionIds.isEmpty()) {
                return Result.error("400", "会话ID列表不能为空");
            }
            
            // 删除会话
            LambdaQueryWrapper<AiChatSession> sessionWrapper = new LambdaQueryWrapper<>();
            sessionWrapper.in(AiChatSession::getSessionId, sessionIds);
            int sessionResult = sessionMapper.delete(sessionWrapper);
            
            // 删除相关消息
            LambdaQueryWrapper<AiChatMessage> messageWrapper = new LambdaQueryWrapper<>();
            messageWrapper.in(AiChatMessage::getSessionId, sessionIds);
            messageMapper.delete(messageWrapper);
            
            return Result.success(sessionResult);
        } catch (Exception e) {
            log.error("批量删除会话失败: ", e);
            return Result.error("500", "批量删除会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取AI配置
     */
    @GetMapping("/config")
    public Result getConfig(@RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            // 从数据库中读取配置
            LambdaQueryWrapper<AiAssistantConfig> wrapper = new LambdaQueryWrapper<>();
            List<AiAssistantConfig> configs = configMapper.selectList(wrapper);
            
            Map<String, Object> configMap = new HashMap<>();
            
            // 设置默认值
            configMap.put("modelName", "deepseek-r1:7b");
            configMap.put("apiUrl", "http://localhost:11434");
            configMap.put("systemPrompt", "你是一个热心、可爱的智能助手，叫哈基聪，请以哈基聪的语气回答问题");
            configMap.put("maxMessages", 20);
            configMap.put("sessionExpireHours", 24);
            configMap.put("enableThinking", true);
            
            // 用数据库中的配置覆盖默认值
            for (AiAssistantConfig config : configs) {
                String key = config.getConfigKey();
                String value = config.getConfigValue();
                
                // 根据配置键的类型进行转换
                switch (key) {
                    case "maxMessages":
                    case "sessionExpireHours":
                        configMap.put(key, Integer.parseInt(value));
                        break;
                    case "enableThinking":
                        configMap.put(key, Boolean.parseBoolean(value));
                        break;
                    default:
                        configMap.put(key, value);
                        break;
                }
            }
            
            return Result.success(configMap);
        } catch (Exception e) {
            log.error("获取AI配置失败: ", e);
            return Result.error("500", "获取配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 保存AI配置
     */
    @PostMapping("/config")
    public Result saveConfig(@RequestBody Map<String, Object> config,
                            @RequestHeader("token") String token) {
        try {
            Account currentUser = TokenUtils.getCurrentUser();
            if (currentUser == null || !"ROLE_ADMIN".equals(currentUser.getRole())) {
                return Result.error("403", "权限不足");
            }
            
            // 保存配置到数据库
            for (Map.Entry<String, Object> entry : config.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                
                // 查询是否已存在该配置
                LambdaQueryWrapper<AiAssistantConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AiAssistantConfig::getConfigKey, key);
                AiAssistantConfig existingConfig = configMapper.selectOne(wrapper);
                
                if (existingConfig != null) {
                    // 更新现有配置
                    existingConfig.setConfigValue(value);
                    existingConfig.setUpdateTime(LocalDateTime.now());
                    configMapper.updateById(existingConfig);
                } else {
                    // 创建新配置
                    AiAssistantConfig newConfig = new AiAssistantConfig();
                    newConfig.setConfigKey(key);
                    newConfig.setConfigValue(value);
                    newConfig.setDescription(getConfigDescription(key));
                    newConfig.setCreateTime(LocalDateTime.now());
                    newConfig.setUpdateTime(LocalDateTime.now());
                    configMapper.insert(newConfig);
                }
            }
            
            // 刷新ChatClient配置
            dynamicChatClientService.refreshChatClient();
            log.info("AI配置已更新并刷新ChatClient: {}", config);
            
            return Result.success("配置保存成功");
        } catch (Exception e) {
            log.error("保存AI配置失败: ", e);
            return Result.error("500", "保存配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取配置项的描述
     */
    private String getConfigDescription(String key) {
        return switch (key) {
            case "modelName" -> "AI模型名称";
            case "apiUrl" -> "API服务地址";
            case "systemPrompt" -> "系统提示词";
            case "maxMessages" -> "最大消息历史数";
            case "sessionExpireHours" -> "会话过期时间（小时）";
            case "enableThinking" -> "是否启用思考显示";
            default -> "AI助手配置项";
        };
    }
}