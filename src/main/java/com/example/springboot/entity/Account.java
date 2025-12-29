package com.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * <p>
 * 账户基类
 * </p>
 */

@Data
public class Account {

    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String avatarUrl;

    @TableField(exist = false)
    private String role;
    @TableField(exist = false)
    private String newPassword;
    @TableField(exist = false)
    private String token;
    
    // 安全问题相关字段（用于注册和找回密码）
    @TableField(exist = false)
    private String securityQuestion;
    @TableField(exist = false)
    private String securityAnswer;

}
