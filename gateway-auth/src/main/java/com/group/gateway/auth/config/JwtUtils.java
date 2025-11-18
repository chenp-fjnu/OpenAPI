package com.group.gateway.auth.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT工具类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class JwtUtils {
    
    @Value("${app.jwt.secret:mySecretKey123456789012345678901234567890}")
    private String jwtSecret;
    
    @Value("${app.jwt.expiration:86400}") // 24小时
    private int jwtExpirationInSec;
    
    @Value("${app.jwt.refresh-expiration:604800}") // 7天
    private int refreshExpirationInSec;
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * 从令牌中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(Claims::getExpiration);
    }
    
    /**
     * 从令牌中获取指定声明
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * 从令牌中获取所有声明
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 检查令牌是否过期
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * 为用户生成令牌
     */
    public String generateToken(String userId, String username, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId);
        claims.put("type", "access");
        return createToken(claims, username);
    }
    
    /**
     * 为用户生成刷新令牌
     */
    public String generateRefreshToken(String userId, String username, String tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tenantId", tenantId);
        claims.put("type", "refresh");
        return createRefreshToken(claims, username);
    }
    
    /**
     * 创建令牌
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInSec * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 创建刷新令牌
     */
    private String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationInSec * 1000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 验证令牌
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = getUsernameFromToken(token);
            return (username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("验证令牌失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("userId", String.class);
    }
    
    /**
     * 从令牌中获取租户ID
     */
    public String getTenantIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("tenantId", String.class);
    }
    
    /**
     * 从令牌中获取令牌类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("type", String.class);
    }
    
    /**
     * 验证令牌类型
     */
    public boolean isAccessToken(String token) {
        String type = getTokenTypeFromToken(token);
        return "access".equals(type);
    }
    
    /**
     * 验证刷新令牌
     */
    public boolean isRefreshToken(String token) {
        String type = getTokenTypeFromToken(token);
        return "refresh".equals(type);
    }
}