package com.group.gateway.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 安全配置类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.security")
public class SecurityConfig {
    
    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();
    
    /**
     * API Key配置
     */
    private ApiKey apiKey = new ApiKey();
    
    /**
     * OAuth配置
     */
    private OAuth oauth = new OAuth();
    
    /**
     * IP白名单配置
     */
    private Whitelist whitelist = new Whitelist();
    
    /**
     * 安全策略配置
     */
    private Policy policy = new Policy();
    
    @Data
    public static class Jwt {
        /**
         * 是否启用JWT验证
         */
        private boolean enabled = true;
        
        /**
         * JWT密钥
         */
        private String secret;
        
        /**
         * JWT过期时间（毫秒）
         */
        private long expiration = 86400000; // 24小时
        
        /**
         * 刷新令牌过期时间（毫秒）
         */
        private long refreshExpiration = 604800000; // 7天
        
        /**
         * JWT头部名称
         */
        private String header = "Authorization";
        
        /**
         * JWT头部前缀
         */
        private String prefix = "Bearer ";
        
        /**
         * 允许的算法列表
         */
        private Set<String> algorithms = Set.of("HS256", "HS384", "HS512");
        
        /**
         * 令牌发行者
         */
        private String issuer = "gateway-service";
        
        /**
         * 令牌受众
         */
        private Set<String> audiences = Set.of("api-clients");
    }
    
    @Data
    public static class ApiKey {
        /**
         * 是否启用API Key验证
         */
        private boolean enabled = true;
        
        /**
         * API Key头部名称
         */
        private String header = "X-API-Key";
        
        /**
         * API Key有效时间（毫秒）
         */
        private long expiration = 86400000; // 24小时
        
        /**
         * API Key前缀
         */
        private String prefix = "sk-";
        
        /**
         * 支持的API Key类型
         */
        private Set<String> supportedTypes = Set.of("api_key", "app_key", "user_key");
    }
    
    @Data
    public static class OAuth {
        /**
         * 是否启用OAuth验证
         */
        private boolean enabled = false;
        
        /**
         * OAuth2服务器地址
         */
        private String serverUrl = "https://oauth.example.com";
        
        /**
         * 客户端ID
         */
        private String clientId;
        
        /**
         * 客户端密钥
         */
        private String clientSecret;
        
        /**
         * OAuth2作用域
         */
        private Set<String> scopes = Set.of("api:read", "api:write");
        
        /**
         * 访问令牌类型
         */
        private String tokenType = "Bearer";
        
        /**
         * 令牌有效时间检查间隔（秒）
         */
        private int tokenValidationInterval = 300;
    }
    
    @Data
    public static class Whitelist {
        /**
         * 是否启用IP白名单
         */
        private boolean enabled = false;
        
        /**
         * 允许的IP列表
         */
        private List<String> ipAddresses = List.of();
        
        /**
         * 允许的IP段列表
         */
        private List<String> ipRanges = List.of();
        
        /**
         * 跳过白名单检查的路径列表
         */
        private List<String> skipPaths = List.of("/api/v1/health", "/api/v1/status");
        
        /**
         * 跳过白名单检查的用户代理列表
         */
        private List<String> skipUserAgents = List.of("health-check", "monitor");
    }
    
    @Data
    public static class Policy {
        /**
         * 最大请求头大小（字节）
         */
        private int maxRequestHeaderSize = 8192; // 8KB
        
        /**
         * 最大请求体大小（字节）
         */
        private int maxRequestBodySize = 10485760; // 10MB
        
        /**
         * 是否要求HTTPS
         */
        private boolean requireHttps = true;
        
        /**
         * 是否记录安全事件
         */
        private boolean logSecurityEvents = true;
        
        /**
         * 安全事件阈值
         */
        private SecurityThresholds thresholds = new SecurityThresholds();
        
        /**
         * 会话配置
         */
        private Session session = new Session();
        
        @Data
        public static class SecurityThresholds {
            /**
             * 每分钟最大失败认证尝试次数
             */
            private int maxFailedAuthAttempts = 5;
            
            /**
             * 每分钟最大请求数
             */
            private int maxRequestsPerMinute = 100;
            
            /**
             * 会话超时时间（秒）
             */
            private int sessionTimeout = 1800; // 30分钟
            
            /**
             * 是否启用自动IP封禁
             */
            private boolean enableAutoBlock = true;
            
            /**
             * IP封禁时间（分钟）
             */
            private int ipBlockDuration = 60;
        }
        
        @Data
        public static class Session {
            /**
             * 是否启用会话管理
             */
            private boolean enabled = true;
            
            /**
             * 会话过期时间（秒）
         */
            private int timeout = 1800;
            
            /**
             * 会话ID头部名称
             */
            private String headerName = "X-Session-ID";
            
            /**
             * 会话Cookie名称
             */
            private String cookieName = "SESSIONID";
            
            /**
             * 是否支持会话续期
             */
            private boolean slidingExpiration = true;
        }
    }
}