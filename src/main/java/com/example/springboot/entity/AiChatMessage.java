package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI聊天消息实体
 */
@Data
@TableName("ai_chat_message")
public class AiChatMessage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;
    
    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;
    
    /**
     * 消息类型 USER-用户消息 ASSISTANT-AI回复
     */
    @TableField("message_type")
    private String messageType;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 创建时间
     */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    /**
     * 是否删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
    
    /**
     * 用户昵称（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    private String userNickname;
    
    /**
     * 消息类型枚举
     */
    public enum MessageType {
        USER("USER", "用户消息"),
        ASSISTANT("ASSISTANT", "AI回复");
        
        private final String code;
        private final String desc;
        
        MessageType(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDesc() {
            return desc;
        }
    }
}