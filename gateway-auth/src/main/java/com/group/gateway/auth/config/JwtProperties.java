package com.group.gateway.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * JWT配置属性
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    
    /**
     * JWT密钥
     */
    private String secret = "mySecretKey123456789012345678901234567890";
    
    /**
     * JWT过期时间（秒）
     */
    private long expiration = 86400; // 24小时
    
    /**
     * 刷新令牌过期时间（秒）
     */
    private long refreshExpiration = 604800; // 7天
    
    /**
     * JWT头部前缀
     */
    private String header = "Bearer ";
    
    /**
     * JWT头部名称
     */
    private String authHeader = "Authorization";
}