package com.example.springboot.controller;

import cn.hutool.core.util.StrUtil;
import com.example.springboot.common.Result;
import com.example.springboot.entity.PasswordReset;
import com.example.springboot.entity.SecurityQuestion;
import com.example.springboot.service.IPasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * 密码重置控制器
 */
@Slf4j
@RestController
@RequestMapping("/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {
    
    private final IPasswordResetService passwordResetService;
    
    /**
     * 获取用户的安全问题
     * @param username 用户名
     * @param userType 用户类型 USER-普通用户 ADMIN-管理员
     * @return 安全问题信息
     */
    @GetMapping("/security-question")
    public Result getSecurityQuestion(@RequestParam String username, 
                                    @RequestParam String userType) {
        try {
            if (StrUtil.isBlank(username) || StrUtil.isBlank(userType)) {
                return Result.error("400", "用户名和用户类型不能为空");
            }
            
            SecurityQuestion response = passwordResetService.getSecurityQuestion(username, userType);
            
            if (response == null) {
                return Result.error("404", "用户不存在");
            }
            
            if (!response.getHasSecurityQuestion()) {
                return Result.error("400", "该用户未设置安全问题，无法通过此方式找回密码");
            }
            
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("获取安全问题失败", e);
            return Result.error("500", "获取安全问题失败，请稍后重试");
        }
    }
    
    /**
     * 验证安全问题答案并重置密码
     * @param request 密码重置请求
     * @param httpRequest HTTP请求
     * @return 重置结果
     */
    @PostMapping("/reset")
    public Result resetPassword(@Valid @RequestBody PasswordReset request,
                               HttpServletRequest httpRequest) {
        try {
            // 参数验证
            if (request == null) {
                return Result.error("400", "请求参数不能为空");
            }
            
            if (StrUtil.isBlank(request.getUsername())) {
                return Result.error("400", "用户名不能为空");
            }
            
            if (StrUtil.isBlank(request.getUserType())) {
                return Result.error("400", "用户类型不能为空");
            }
            
            if (StrUtil.isBlank(request.getSecurityAnswer())) {
                return Result.error("400", "安全问题答案不能为空");
            }
            
            if (StrUtil.isBlank(request.getNewPassword())) {
                return Result.error("400", "新密码不能为空");
            }
            
            if (StrUtil.isBlank(request.getConfirmPassword())) {
                return Result.error("400", "确认密码不能为空");
            }
            
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return Result.error("400", "新密码和确认密码不一致");
            }
            
            // 执行密码重置
            boolean success = passwordResetService.resetPassword(request, httpRequest);
            
            if (success) {
                log.info("密码重置成功: username={}, userType={}", 
                        request.getUsername(), request.getUserType());
                return Result.success("密码重置成功");
            } else {
                return Result.error("400", "密码重置失败，请检查安全问题答案是否正确");
            }
            
        } catch (Exception e) {
            log.error("密码重置失败", e);
            return Result.error("500", "密码重置失败，请稍后重试");
        }
    }
    
    /**
     * 验证安全问题答案（不重置密码，仅验证）
     * @param username 用户名
     * @param userType 用户类型
     * @param answer 安全问题答案
     * @return 验证结果
     */
    @PostMapping("/verify-answer")
    public Result verifySecurityAnswer(@RequestParam String username,
                                     @RequestParam String userType,
                                     @RequestParam String answer) {
        try {
            if (StrUtil.isBlank(username) || StrUtil.isBlank(userType) || StrUtil.isBlank(answer)) {
                return Result.error("400", "参数不能为空");
            }
            
            boolean isValid = passwordResetService.verifySecurityAnswer(username, userType, answer);
            
            if (isValid) {
                return Result.success("验证通过");
            } else {
                return Result.error("400", "安全问题答案错误");
            }
            
        } catch (Exception e) {
            log.error("验证安全问题答案失败", e);
            return Result.error("500", "验证失败，请稍后重试");
        }
    }
    
    /**
     * 设置安全问题（用于注册时或用户主动设置）
     * @param userId 用户ID
     * @param userType 用户类型
     * @param question 安全问题
     * @param answer 安全问题答案
     * @return 设置结果
     */
    @PostMapping("/set-security-question")
    public Result setSecurityQuestion(@RequestParam Integer userId,
                                    @RequestParam String userType,
                                    @RequestParam String question,
                                    @RequestParam String answer) {
        try {
            if (userId == null || StrUtil.isBlank(userType) || 
                StrUtil.isBlank(question) || StrUtil.isBlank(answer)) {
                return Result.error("400", "参数不能为空");
            }
            
            boolean success = passwordResetService.setSecurityQuestion(userId, userType, question, answer);
            
            if (success) {
                return Result.success("安全问题设置成功");
            } else {
                return Result.error("400", "安全问题设置失败");
            }
            
        } catch (Exception e) {
            log.error("设置安全问题失败", e);
            return Result.error("500", "设置失败，请稍后重试");
        }
    }
}
