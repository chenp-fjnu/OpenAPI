package com.group.gateway.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT工具类
 * 提供JWT令牌生成、验证和解析功能
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${gateway.app.jwtSecret}")
    private String jwtSecret;
    
    @Value("${gateway.app.jwtExpirationMs}")
    private int jwtExpirationMs;
    
    @Value("${gateway.app.jwtRefreshExpirationMs}")
    private int jwtRefreshExpirationMs;
    
    /**
     * 生成JWT令牌
     */
    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateTokenFromUsername(userPrincipal.getUsername());
    }
    
    /**
     * 根据用户名生成JWT令牌
     */
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtRefreshExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    /**
     * 验证JWT令牌
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("无效的JWT令牌: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT令牌已过期: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("不支持的JWT令牌: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT令牌为空: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT令牌验证失败: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 获取令牌的过期时间
     */
    public Date getExpirationDateFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }
    
    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromJwtToken(token);
        return expiration.before(new Date());
    }
    
    /**
     * 从令牌中获取声明
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("从令牌中获取声明失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从令牌中获取特定声明值
     */
    public String getClaimFromToken(String token, String claimName) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.get(claimName, String.class) : null;
    }
    
    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 生成令牌剩余有效时间（毫秒）
     */
    public long getRemainingValidityTime(String token) {
        try {
            Date expiration = getExpirationDateFromJwtToken(token);
            return expiration.getTime() - System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("计算令牌剩余有效时间失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 检查令牌是否即将过期（阈值：5分钟）
     */
    public boolean isTokenExpiringSoon(String token) {
        return getRemainingValidityTime(token) < 5 * 60 * 1000; // 5分钟
    }
}