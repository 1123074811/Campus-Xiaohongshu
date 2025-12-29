package com.example.springboot.service;

import com.example.springboot.entity.PasswordReset;
import com.example.springboot.entity.SecurityQuestion;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 密码重置服务接口
 */
public interface IPasswordResetService {
    
    /**
     * 根据用户名获取安全问题
     * @param username 用户名
     * @param userType 用户类型 USER-普通用户 ADMIN-管理员
     * @return 安全问题信息
     */
    SecurityQuestion getSecurityQuestion(String username, String userType);
    
    /**
     * 验证安全问题答案并重置密码
     * @param request 密码重置请求
     * @param httpRequest HTTP请求（用于获取IP等信息）
     * @return 是否重置成功
     */
    boolean resetPassword(PasswordReset request, HttpServletRequest httpRequest);
    
    /**
     * 验证安全问题答案
     * @param username 用户名
     * @param userType 用户类型
     * @param answer 答案
     * @return 是否验证通过
     */
    boolean verifySecurityAnswer(String username, String userType, String answer);
    
    /**
     * 设置用户安全问题
     * @param userId 用户ID
     * @param userType 用户类型
     * @param question 安全问题
     * @param answer 安全问题答案
     * @return 是否设置成功
     */
    boolean setSecurityQuestion(Integer userId, String userType, String question, String answer);
}