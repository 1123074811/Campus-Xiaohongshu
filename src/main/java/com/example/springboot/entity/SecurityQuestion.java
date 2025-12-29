package com.example.springboot.entity;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 安全问题实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SecurityQuestion {
    
    /**
     * 用户ID
     */
    private Integer userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 安全问题
     */
    private String securityQuestion;
    
    /**
     * 是否设置了安全问题
     */
    private Boolean hasSecurityQuestion;
    
    public SecurityQuestion(Integer userId, String username, String securityQuestion) {
        this.userId = userId;
        this.username = username;
        this.securityQuestion = securityQuestion;
        this.hasSecurityQuestion = securityQuestion != null && !securityQuestion.trim().isEmpty();
    }
}
