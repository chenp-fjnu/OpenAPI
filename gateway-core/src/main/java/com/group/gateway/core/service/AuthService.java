package com.group.gateway.core.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务
 * 负责JWT令牌验证、用户身份验证和权限检查
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    @Value("${gateway.auth.jwt.secret:mySecretKey123456789012345678901234567890}")
    private String jwtSecret;
    
    @Value("${gateway.auth.jwt.expiration:3600}")
    private long jwtExpiration;
    
    @Value("${gateway.auth.redis.prefix:auth:}")
    private String redisPrefix;
    
    private final StringRedisTemplate redisTemplate;
    private final RequestContextService requestContextService;
    
    /**
     * 验证认证信息
     */
    public Mono<AuthFilter.AuthResult> validateAuth(ServerWebExchange exchange, String token, String userId) {
        // 如果没有token且有userId，可能是内部服务调用
        if (!StringUtils.hasText(token) && StringUtils.hasText(userId)) {
            return validateInternalUser(exchange, userId);
        }
        
        // 验证JWT令牌
        if (StringUtils.hasText(token)) {
            return validateJwtToken(exchange, token);
        }
        
        // 无认证信息，返回失败
        return Mono.just(AuthFilter.AuthResult.failure("缺少认证信息"));
    }
    
    /**
     * 验证内部用户（来自其他服务的调用）
     */
    private Mono<AuthFilter.AuthResult> validateInternalUser(ServerWebExchange exchange, String userId) {
        return Mono.fromCallable(() -> {
            try {
                // 从Redis中获取用户信息
                String userInfoKey = redisPrefix + "internal_user:" + userId;
                String userInfo = redisTemplate.opsForValue().get(userInfoKey);
                
                if (StringUtils.hasText(userInfo)) {
                    // 解析用户信息（假设为JSON格式）
                    Map<String, Object> userInfoMap = parseUserInfo(userInfo);
                    
                    return AuthFilter.AuthResult.success(
                        userId,
                        getString(userInfoMap, "tenantId"),
                        getString(userInfoMap, "roles"),
                        getString(userInfoMap, "clientId"),
                        generateTraceId(exchange)
                    );
                } else {
                    return AuthFilter.AuthResult.failure("无效的用户ID");
                }
            } catch (Exception e) {
                log.error("验证内部用户失败", e);
                return AuthFilter.AuthResult.failure("认证服务异常");
            }
        });
    }
    
    /**
     * 验证JWT令牌
     */
    private Mono<AuthFilter.AuthResult> validateJwtToken(ServerWebExchange exchange, String token) {
        return Mono.fromCallable(() -> {
            try {
                // 解析JWT令牌
                Claims claims = parseJwtToken(token);
                
                if (claims != null) {
                    String userId = claims.getSubject();
                    String tenantId = claims.get("tenantId", String.class);
                    String roles = claims.get("roles", String.class);
                    String clientId = claims.get("clientId", String.class);
                    
                    // 验证令牌是否在黑名单中
                    if (isTokenBlacklisted(token)) {
                        log.warn("令牌在黑名单中 | userId={} | jti={}", userId, claims.getId());
                        return AuthFilter.AuthResult.failure("令牌已失效");
                    }
                    
                    return AuthFilter.AuthResult.success(
                        userId,
                        tenantId,
                        roles,
                        clientId,
                        generateTraceId(exchange)
                    );
                } else {
                    return AuthFilter.AuthResult.failure("无效的令牌");
                }
            } catch (JwtException e) {
                log.warn("JWT令牌验证失败 | reason={}", e.getMessage());
                return AuthFilter.AuthResult.failure("令牌验证失败: " + e.getMessage());
            } catch (Exception e) {
                log.error("令牌验证过程中发生异常", e);
                return AuthFilter.AuthResult.failure("认证服务异常");
            }
        });
    }
    
    /**
     * 解析JWT令牌
     */
    private Claims parseJwtToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            
            Jws<Claims> jws = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            
            return jws.getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("令牌已过期", e);
            throw new JwtException("令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            log.debug("不支持的令牌格式", e);
            throw new JwtException("不支持的令牌格式", e);
        } catch (MalformedJwtException e) {
            log.debug("令牌格式错误", e);
            throw new JwtException("令牌格式错误", e);
        } catch (SecurityException | IllegalArgumentException e) {
            log.debug("令牌验证失败", e);
            throw new JwtException("令牌验证失败", e);
        }
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String token) {
        try {
            Claims claims = parseJwtToken(token);
            String jti = claims.getId();
            
            if (StringUtils.hasText(jti)) {
                String blacklistKey = redisPrefix + "blacklist:" + jti;
                return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
            }
            return false;
        } catch (Exception e) {
            // 如果无法解析令牌，假设不在黑名单中
            return false;
        }
    }
    
    /**
     * 生成访问令牌
     */
    public Mono<String> generateAccessToken(String userId, String tenantId, String[] roles, String clientId) {
        return Mono.fromCallable(() -> {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                
                Map<String, Object> claims = new HashMap<>();
                claims.put("tenantId", tenantId);
                claims.put("roles", String.join(",", roles));
                claims.put("clientId", clientId);
                claims.put("type", "access");
                
                String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId)
                    .setIssuedAt(new java.util.Date())
                    .setExpiration(new java.util.Date(System.currentTimeMillis() + jwtExpiration * 1000))
                    .signWith(key)
                    .compact();
                
                return token;
            } catch (Exception e) {
                log.error("生成访问令牌失败", e);
                throw new RuntimeException("生成令牌失败", e);
            }
        });
    }
    
    /**
     * 生成刷新令牌
     */
    public Mono<String> generateRefreshToken(String userId, String clientId) {
        return Mono.fromCallable(() -> {
            try {
                SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                
                Map<String, Object> claims = new HashMap<>();
                claims.put("type", "refresh");
                claims.put("clientId", clientId);
                
                String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId)
                    .setIssuedAt(new java.util.Date())
                    .setExpiration(new java.util.Date(System.currentTimeMillis() + (jwtExpiration * 24) * 1000)) // 24小时
                    .signWith(key)
                    .compact();
                
                return token;
            } catch (Exception e) {
                log.error("生成刷新令牌失败", e);
                throw new RuntimeException("生成刷新令牌失败", e);
            }
        });
    }
    
    /**
     * 将令牌添加到黑名单
     */
    public Mono<Void> blacklistToken(String token) {
        return Mono.fromRunnable(() -> {
            try {
                Claims claims = parseJwtToken(token);
                String jti = claims.getId();
                
                if (StringUtils.hasText(jti)) {
                    String blacklistKey = redisPrefix + "blacklist:" + jti;
                    long expiration = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                    
                    if (expiration > 0) {
                        redisTemplate.opsForValue().set(blacklistKey, "1", expiration, TimeUnit.SECONDS);
                        log.info("令牌已加入黑名单 | jti={} | expiration={}s", jti, expiration);
                    }
                }
            } catch (Exception e) {
                log.error("将令牌加入黑名单失败", e);
            }
        });
    }
    
    /**
     * 验证用户权限
     */
    public Mono<Boolean> validatePermission(String userId, String requiredPermission) {
        return Mono.fromCallable(() -> {
            try {
                String userPermissionsKey = redisPrefix + "user_permissions:" + userId;
                String permissions = redisTemplate.opsForValue().get(userPermissionsKey);
                
                if (StringUtils.hasText(permissions)) {
                    String[] userPermissions = permissions.split(",");
                    return Arrays.asList(userPermissions).contains(requiredPermission);
                }
                
                return false;
            } catch (Exception e) {
                log.error("验证用户权限失败", e);
                return false;
            }
        });
    }
    
    /**
     * 解析用户信息字符串
     */
    private Map<String, Object> parseUserInfo(String userInfo) {
        // 简化实现，实际应该使用JSON解析
        Map<String, Object> result = new HashMap<>();
        String[] lines = userInfo.split("\n");
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                result.put(parts[0].trim(), parts[1].trim());
            }
        }
        return result;
    }
    
    /**
     * 从Map中获取字符串值
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * 生成链路追踪ID
     */
    private String generateTraceId(ServerWebExchange exchange) {
        return requestContextService.generateTraceId(exchange);
    }
}