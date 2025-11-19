package com.group.gateway.ratelimit.strategy;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 限流结果
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {
    
    /**
     * 是否允许请求
     */
    private boolean allowed;
    
    /**
     * 剩余可用数量
     */
    private long remaining;
    
    /**
     * 重置时间（Unix时间戳，毫秒）
     */
    private long resetTime;
    
    /**
     * 当前窗口内请求数量
     */
    private long requestCount;
    
    /**
     * 限流策略名称
     */
    private String strategyName;
    
    /**
     * 超出限流时返回的HTTP状态码
     */
    private int statusCode;
    
    /**
     * 限流消息
     */
    private String message;
    
    /**
     * 创建允许的限流结果
     *
     * @param remaining 剩余可用数量
     * @param resetTime 重置时间
     * @param requestCount 当前请求数量
     * @param strategyName 策略名称
     * @return 限流结果
     */
    public static RateLimitResult allowed(long remaining, long resetTime, long requestCount, String strategyName) {
        return RateLimitResult.builder()
                .allowed(true)
                .remaining(remaining)
                .resetTime(resetTime)
                .requestCount(requestCount)
                .strategyName(strategyName)
                .build();
    }
    
    /**
     * 创建拒绝的限流结果
     *
     * @param remaining 剩余可用数量
     * @param resetTime 重置时间
     * @param requestCount 当前请求数量
     * @param strategyName 策略名称
     * @param statusCode HTTP状态码
     * @param message 限流消息
     * @return 限流结果
     */
    public static RateLimitResult rejected(long remaining, long resetTime, long requestCount, 
                                         String strategyName, int statusCode, String message) {
        return RateLimitResult.builder()
                .allowed(false)
                .remaining(remaining)
                .resetTime(resetTime)
                .requestCount(requestCount)
                .strategyName(strategyName)
                .statusCode(statusCode)
                .message(message)
                .build();
    }
}