package com.example.springboot.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.springboot.entity.Admin;
import com.example.springboot.entity.PasswordReset;
import com.example.springboot.entity.SecurityQuestion;
import com.example.springboot.entity.User;
import com.example.springboot.mapper.AdminMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IPasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 密码重置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements IPasswordResetService {
    
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    
    @Override
    public SecurityQuestion getSecurityQuestion(String username, String userType) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(userType)) {
            return null;
        }
        
        try {
            if ("USER".equalsIgnoreCase(userType)) {
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getUsername, username);
                User user = userMapper.selectOne(wrapper);
                
                if (user != null) {
                    return new SecurityQuestion(user.getId(), user.getUsername(), user.getSecurityQuestion());
                }
            } else if ("ADMIN".equalsIgnoreCase(userType)) {
                LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Admin::getUsername, username);
                Admin admin = adminMapper.selectOne(wrapper);
                
                if (admin != null) {
                    return new SecurityQuestion(admin.getId(), admin.getUsername(), admin.getSecurityQuestion());
                }
            }
        } catch (Exception e) {
            log.error("获取安全问题失败: username={}, userType={}", username, userType, e);
        }
        
        return null;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(PasswordReset request, HttpServletRequest httpRequest) {
        // 参数验证
        if (request == null || StrUtil.isBlank(request.getUsername()) || 
            StrUtil.isBlank(request.getUserType()) || StrUtil.isBlank(request.getNewPassword()) ||
            StrUtil.isBlank(request.getSecurityAnswer())) {
            log.warn("密码重置参数不完整");
            return false;
        }
        
        // 验证新密码和确认密码是否一致
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("新密码和确认密码不一致");
            return false;
        }
        
        try {
            // 验证安全问题答案
            if (!verifySecurityAnswer(request.getUsername(), request.getUserType(), request.getSecurityAnswer())) {
                log.warn("安全问题答案验证失败: username={}", request.getUsername());
                return false;
            }
            
            // 重置密码
            boolean resetSuccess = false;
            Integer userId = null;
            
            // 对新密码进行MD5加密
            String encryptedPassword = DigestUtil.md5Hex(request.getNewPassword());
            
            if ("USER".equalsIgnoreCase(request.getUserType())) {
                LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(User::getUsername, request.getUsername())
                       .set(User::getPassword, encryptedPassword)
                       .set(User::getUpdateTime, LocalDateTime.now());
                
                int updateCount = userMapper.update(null, wrapper);
                resetSuccess = updateCount > 0;
                
                if (resetSuccess) {
                    // 获取用户ID用于日志记录
                    LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(User::getUsername, request.getUsername());
                    User user = userMapper.selectOne(queryWrapper);
                    if (user != null) {
                        userId = user.getId();
                    }
                }
            } else if ("ADMIN".equalsIgnoreCase(request.getUserType())) {
                LambdaUpdateWrapper<Admin> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Admin::getUsername, request.getUsername())
                       .set(Admin::getPassword, encryptedPassword)
                       .set(Admin::getUpdateTime, LocalDateTime.now());
                
                int updateCount = adminMapper.update(null, wrapper);
                resetSuccess = updateCount > 0;
                
                if (resetSuccess) {
                    // 获取管理员ID用于日志记录
                    LambdaQueryWrapper<Admin> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(Admin::getUsername, request.getUsername());
                    Admin admin = adminMapper.selectOne(queryWrapper);
                    if (admin != null) {
                        userId = admin.getId();
                    }
                }
            }
            
            // 记录密码重置成功日志
            if (resetSuccess && userId != null) {
                log.info("密码重置成功: username={}, userType={}", request.getUsername(), request.getUserType());
            }
            
            return resetSuccess;
            
        } catch (Exception e) {
            log.error("密码重置失败: username={}, userType={}", request.getUsername(), request.getUserType(), e);
            return false;
        }
    }
    
    @Override
    public boolean verifySecurityAnswer(String username, String userType, String answer) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(userType) || StrUtil.isBlank(answer)) {
            return false;
        }
        
        try {
            // 对答案进行MD5加密，与数据库中存储的加密答案比较
            String encryptedAnswer = DigestUtil.md5Hex(answer.trim().toLowerCase());
            
            if ("USER".equalsIgnoreCase(userType)) {
                LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(User::getUsername, username);
                User user = userMapper.selectOne(wrapper);
                
                if (user != null && StrUtil.isNotBlank(user.getSecurityAnswer())) {
                    return encryptedAnswer.equals(user.getSecurityAnswer());
                }
            } else if ("ADMIN".equalsIgnoreCase(userType)) {
                LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Admin::getUsername, username);
                Admin admin = adminMapper.selectOne(wrapper);
                
                if (admin != null && StrUtil.isNotBlank(admin.getSecurityAnswer())) {
                    return encryptedAnswer.equals(admin.getSecurityAnswer());
                }
            }
        } catch (Exception e) {
            log.error("验证安全问题答案失败: username={}, userType={}", username, userType, e);
        }
        
        return false;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setSecurityQuestion(Integer userId, String userType, String question, String answer) {
        if (userId == null || StrUtil.isBlank(userType) || StrUtil.isBlank(question) || StrUtil.isBlank(answer)) {
            return false;
        }
        
        try {
            // 对答案进行MD5加密存储
            String encryptedAnswer = DigestUtil.md5Hex(answer.trim().toLowerCase());
            
            if ("USER".equalsIgnoreCase(userType)) {
                LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(User::getId, userId)
                       .set(User::getSecurityQuestion, question)
                       .set(User::getSecurityAnswer, encryptedAnswer)
                       .set(User::getUpdateTime, LocalDateTime.now());
                
                return userMapper.update(null, wrapper) > 0;
            } else if ("ADMIN".equalsIgnoreCase(userType)) {
                LambdaUpdateWrapper<Admin> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Admin::getId, userId)
                       .set(Admin::getSecurityQuestion, question)
                       .set(Admin::getSecurityAnswer, encryptedAnswer)
                       .set(Admin::getUpdateTime, LocalDateTime.now());
                
                return adminMapper.update(null, wrapper) > 0;
            }
        } catch (Exception e) {
            log.error("设置安全问题失败: userId={}, userType={}", userId, userType, e);
        }
        
        return false;
    }
}