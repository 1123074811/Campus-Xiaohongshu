package com.example.springboot.entity;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 密码重置实体类
 */
@Data
public class PasswordReset {
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    /**
     * 用户类型 USER-普通用户 ADMIN-管理员
     */
    @NotBlank(message = "用户类型不能为空")
    private String userType;
    
    /**
     * 安全问题答案
     */
    @NotBlank(message = "安全问题答案不能为空")
    private String securityAnswer;
    
    /**
     * 新密码
     */
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    
    /**
     * 确认新密码
     */
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
