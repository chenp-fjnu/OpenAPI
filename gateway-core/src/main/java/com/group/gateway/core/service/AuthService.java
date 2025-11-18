package com.group.gateway.core.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class AuthService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RequestContextService requestContextService;
    
    @Value("${gateway.jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    @Value("${gateway.jwt.expiration:86400}")
    private long jwtExpiration;
    
    @Value("${gateway.jwt.refresh-expiration:604800}")
    private long refreshTokenExpiration;
    
    @Value("${gateway.auth.blacklist.enabled:true}")
    private boolean blacklistEnabled;
    
    @Value("${gateway.auth.internal.enabled:true}")
    private boolean internalAuthEnabled;
    
    @Value("${gateway.auth.default-role:USER}")
    private String defaultRole;
    
    @Value("${gateway.auth.internal.users}")
    private String internalUsers; // 格式: user1:password1:role1,user2:password2:role2
    
    private static final String USER_INFO_PREFIX = "user:info:";
    private static final String USER_BLACKLIST_PREFIX = "user:blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final String ACCESS_TOKEN_PREFIX = "access:token:";
    
    /**
     * JWT令牌生成
     */
    public Mono<String> generateAccessToken(AuthInfo authInfo) {
        return Mono.fromCallable(() -> {
            try {
                Instant now = Instant.now();
                Instant expiry = now.plusSeconds(jwtExpiration);
                
                Map<String, Object> claims = new HashMap<>();
                claims.put("userId", authInfo.getUserId());
                claims.put("tenantId", authInfo.getTenantId());
                claims.put("roles", authInfo.getRoles());
                claims.put("permissions", authInfo.getPermissions());
                claims.put("email", authInfo.getEmail());
                claims.put("nickname", authInfo.getNickname());
                claims.put("tokenType", "access");
                
                String token = Jwts.builder()
                    .setSubject(authInfo.getUserId())
                    .addClaims(claims)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiry))
                    .signWith(getSigningKey())
                    .compact();
                
                // 将令牌信息存储到Redis
                String tokenKey = ACCESS_TOKEN_PREFIX + token;
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("userId", authInfo.getUserId());
                tokenInfo.put("tenantId", authInfo.getTenantId());
                tokenInfo.put("roles", authInfo.getRoles());
                tokenInfo.put("generated", now.toString());
                tokenInfo.put("expires", expiry.toString());
                
                redisTemplate.opsForValue().set(tokenKey, tokenInfo, jwtExpiration, TimeUnit.SECONDS);
                
                log.debug("生成访问令牌成功 - 用户ID: {}, 租户ID: {}", authInfo.getUserId(), authInfo.getTenantId());
                
                return token;
                
            } catch (Exception e) {
                log.error("生成访问令牌失败 - 用户ID: {}", authInfo.getUserId(), e);
                throw new RuntimeException("令牌生成失败", e);
            }
        });
    }
    
    /**
     * 刷新令牌生成
     */
    public Mono<String> generateRefreshToken(String userId) {
        return Mono.fromCallable(() -> {
            try {
                Instant now = Instant.now();
                Instant expiry = now.plusSeconds(refreshTokenExpiration);
                
                String token = Jwts.builder()
                    .setSubject(userId)
                    .claim("tokenType", "refresh")
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiry))
                    .signWith(getSigningKey())
                    .compact();
                
                // 将刷新令牌存储到Redis
                String tokenKey = REFRESH_TOKEN_PREFIX + token;
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("userId", userId);
                tokenInfo.put("generated", now.toString());
                tokenInfo.put("expires", expiry.toString());
                
                redisTemplate.opsForValue().set(tokenKey, tokenInfo, refreshTokenExpiration, TimeUnit.SECONDS);
                
                log.debug("生成刷新令牌成功 - 用户ID: {}", userId);
                
                return token;
                
            } catch (Exception e) {
                log.error("生成刷新令牌失败 - 用户ID: {}", userId, e);
                throw new RuntimeException("刷新令牌生成失败", e);
            }
        });
    }
    
    /**
     * 验证访问令牌
     */
    public Mono<AuthInfo> validateAccessToken(String token) {
        return Mono.fromCallable(() -> {
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("令牌不能为空");
            }
            
            try {
                // 检查是否在黑名单中
                if (blacklistEnabled && isTokenBlacklisted(token)) {
                    throw new IllegalArgumentException("令牌已被列入黑名单");
                }
                
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
                
                Claims claims = claimsJws.getBody();
                
                // 验证令牌类型
                String tokenType = claims.get("tokenType", String.class);
                if (!"access".equals(tokenType)) {
                    throw new IllegalArgumentException("无效的令牌类型");
                }
                
                // 构建认证信息
                AuthInfo authInfo = new AuthInfo();
                authInfo.setUserId(claims.getSubject());
                authInfo.setTenantId(claims.get("tenantId", String.class));
                authInfo.setEmail(claims.get("email", String.class));
                authInfo.setNickname(claims.get("nickname", String.class));
                authInfo.setRoles(claims.get("roles", List.class));
                authInfo.setPermissions(claims.get("permissions", List.class));
                authInfo.setTokenType(tokenType);
                
                log.debug("验证访问令牌成功 - 用户ID: {}, 租户ID: {}", 
                    authInfo.getUserId(), authInfo.getTenantId());
                
                return authInfo;
                
            } catch (ExpiredJwtException e) {
                log.warn("令牌已过期: {}", e.getMessage());
                throw new IllegalArgumentException("令牌已过期");
            } catch (UnsupportedJwtException e) {
                log.warn("不支持的令牌: {}", e.getMessage());
                throw new IllegalArgumentException("不支持的令牌格式");
            } catch (MalformedJwtException e) {
                log.warn("无效的令牌: {}", e.getMessage());
                throw new IllegalArgumentException("令牌格式无效");
            } catch (SecurityException | IllegalArgumentException e) {
                log.warn("令牌验证失败: {}", e.getMessage());
                throw new IllegalArgumentException("令牌验证失败");
            }
        });
    }
    
    /**
     * 验证刷新令牌
     */
    public Mono<AuthInfo> validateRefreshToken(String token) {
        return Mono.fromCallable(() -> {
            if (!StringUtils.hasText(token)) {
                throw new IllegalArgumentException("刷新令牌不能为空");
            }
            
            try {
                // 检查是否在黑名单中
                if (blacklistEnabled && isTokenBlacklisted(token)) {
                    throw new IllegalArgumentException("刷新令牌已被列入黑名单");
                }
                
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
                
                Claims claims = claimsJws.getBody();
                
                // 验证令牌类型
                String tokenType = claims.get("tokenType", String.class);
                if (!"refresh".equals(tokenType)) {
                    throw new IllegalArgumentException("无效的令牌类型");
                }
                
                String userId = claims.getSubject();
                
                // 构建认证信息
                AuthInfo authInfo = new AuthInfo();
                authInfo.setUserId(userId);
                authInfo.setTokenType(tokenType);
                
                log.debug("验证刷新令牌成功 - 用户ID: {}", userId);
                
                return authInfo;
                
            } catch (ExpiredJwtException e) {
                log.warn("刷新令牌已过期: {}", e.getMessage());
                throw new IllegalArgumentException("刷新令牌已过期");
            } catch (UnsupportedJwtException | MalformedJwtException e) {
                log.warn("无效的刷新令牌: {}", e.getMessage());
                throw new IllegalArgumentException("刷新令牌格式无效");
            } catch (SecurityException | IllegalArgumentException e) {
                log.warn("刷新令牌验证失败: {}", e.getMessage());
                throw new IllegalArgumentException("刷新令牌验证失败");
            }
        });
    }
    
    /**
     * 验证内部用户
     */
    public Mono<AuthInfo> validateInternalUser(String username, String password) {
        return Mono.fromCallable(() -> {
            if (!internalAuthEnabled) {
                throw new IllegalArgumentException("内部认证已禁用");
            }
            
            if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
                throw new IllegalArgumentException("用户名和密码不能为空");
            }
            
            try {
                Map<String, String> internalUserMap = parseInternalUsers();
                String userConfig = internalUserMap.get(username);
                
                if (userConfig == null) {
                    throw new IllegalArgumentException("用户不存在");
                }
                
                String[] parts = userConfig.split(":");
                if (parts.length < 2) {
                    log.error("内部用户配置错误: {}", username);
                    throw new IllegalArgumentException("用户配置错误");
                }
                
                String storedPassword = parts[0];
                String role = parts.length > 2 ? parts[2] : defaultRole;
                
                // 验证密码（这里应该使用密码编码器进行安全验证）
                if (!password.equals(storedPassword)) {
                    log.warn("内部用户密码验证失败: {}", username);
                    throw new IllegalArgumentException("密码错误");
                }
                
                // 构建认证信息
                AuthInfo authInfo = new AuthInfo();
                authInfo.setUserId(username);
                authInfo.setEmail(username + "@internal.local");
                authInfo.setNickname(username);
                authInfo.setRoles(Arrays.asList(role));
                authInfo.setTokenType("internal");
                authInfo.setTenantId("internal");
                
                log.debug("验证内部用户成功: {}", username);
                
                return authInfo;
                
            } catch (Exception e) {
                log.error("验证内部用户异常: {}", username, e);
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new IllegalArgumentException("内部认证失败");
            }
        });
    }
    
    /**
     * 将令牌加入黑名单
     */
    public Mono<Void> blacklistToken(String token, String reason) {
        return Mono.fromRunnable(() -> {
            if (!blacklistEnabled || !StringUtils.hasText(token)) {
                return;
            }
            
            try {
                String blacklistKey = USER_BLACKLIST_PREFIX + token;
                
                // 解析令牌获取过期时间
                Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
                
                Date expiration = claims.getExpiration();
                long ttl = Math.max(expiration.getTime() - System.currentTimeMillis(), 1000);
                
                Map<String, Object> blacklistInfo = new HashMap<>();
                blacklistInfo.put("reason", reason);
                blacklistInfo.put("blacklistedAt", Instant.now().toString());
                
                redisTemplate.opsForValue().set(blacklistKey, blacklistInfo, ttl, TimeUnit.MILLISECONDS);
                
                log.debug("令牌加入黑名单 - 令牌: {}, 原因: {}, 过期时间: {}秒", 
                    token.substring(0, Math.min(20, token.length())) + "...", reason, ttl / 1000);
                
            } catch (Exception e) {
                log.error("令牌加入黑名单失败: {}", token.substring(0, Math.min(20, token.length())) + "...", e);
            }
        }).then();
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            String blacklistKey = USER_BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.error("检查黑名单状态失败: {}", token.substring(0, Math.min(20, token.length())) + "...", e);
            return false;
        }
    }
    
    /**
     * 检查用户权限
     */
    public boolean hasPermission(AuthInfo authInfo, String permission) {
        if (authInfo == null || authInfo.getPermissions() == null) {
            return false;
        }
        
        // 超级管理员拥有所有权限
        if (authInfo.getPermissions().contains("*")) {
            return true;
        }
        
        return authInfo.getPermissions().contains(permission);
    }
    
    /**
     * 检查用户角色
     */
    public boolean hasRole(AuthInfo authInfo, String role) {
        if (authInfo == null || authInfo.getRoles() == null) {
            return false;
        }
        
        return authInfo.getRoles().contains(role);
    }
    
    /**
     * 检查用户是否有任意角色
     */
    public boolean hasAnyRole(AuthInfo authInfo, String... roles) {
        if (authInfo == null || authInfo.getRoles() == null) {
            return false;
        }
        
        return authInfo.getRoles().stream()
            .anyMatch(role -> Arrays.asList(roles).contains(role));
    }
    
    /**
     * 获取JWT签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 解析内部用户配置
     */
    private Map<String, String> parseInternalUsers() {
        Map<String, String> userMap = new HashMap<>();
        
        if (!StringUtils.hasText(internalUsers)) {
            return userMap;
        }
        
        String[] userConfigs = internalUsers.split(",");
        for (String userConfig : userConfigs) {
            String[] parts = userConfig.trim().split(":");
            if (parts.length >= 2) {
                userMap.put(parts[0].trim(), userConfig.trim());
            }
        }
        
        return userMap;
    }
    
    /**
     * 清理过期的令牌
     */
    public void cleanupExpiredTokens() {
        try {
            // 这里可以实现清理过期令牌的逻辑
            // 目前主要依赖于Redis的TTL自动过期
            log.debug("开始清理过期令牌");
        } catch (Exception e) {
            log.error("清理过期令牌失败", e);
        }
    }
    
    /**
     * 获取认证统计信息
     */
    public Map<String, Object> getAuthStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("jwtExpiration", jwtExpiration);
        stats.put("refreshTokenExpiration", refreshTokenExpiration);
        stats.put("blacklistEnabled", blacklistEnabled);
        stats.put("internalAuthEnabled", internalAuthEnabled);
        
        // 统计内部用户数量
        Map<String, String> internalUsersMap = parseInternalUsers();
        stats.put("internalUsersCount", internalUsersMap.size());
        
        // 获取Redis中的令牌统计
        try {
            Set<String> accessTokenKeys = redisTemplate.keys(ACCESS_TOKEN_PREFIX + "*");
            Set<String> refreshTokenKeys = redisTemplate.keys(REFRESH_TOKEN_PREFIX + "*");
            Set<String> blacklistKeys = redisTemplate.keys(USER_BLACKLIST_PREFIX + "*");
            
            stats.put("activeAccessTokens", accessTokenKeys != null ? accessTokenKeys.size() : 0);
            stats.put("activeRefreshTokens", refreshTokenKeys != null ? refreshTokenKeys.size() : 0);
            stats.put("blacklistedTokens", blacklistKeys != null ? blacklistKeys.size() : 0);
            
        } catch (Exception e) {
            log.warn("获取令牌统计失败", e);
            stats.put("activeAccessTokens", -1);
            stats.put("activeRefreshTokens", -1);
            stats.put("blacklistedTokens", -1);
        }
        
        return stats;
    }
}