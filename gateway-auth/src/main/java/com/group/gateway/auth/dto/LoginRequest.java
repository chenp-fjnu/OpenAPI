package com.group.gateway.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 登录请求DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    private String tenantId;
    
    private String clientId;
    
    private String clientSecret;
    
    private boolean rememberMe = false;
    
    private String captchaToken;
    
    private String captchaAnswer;
    
    private String loginType; // username, email, phone
    
    private String deviceId;
    
    private String deviceInfo;
    
    private String ipAddress;
    
    private String userAgent;
}