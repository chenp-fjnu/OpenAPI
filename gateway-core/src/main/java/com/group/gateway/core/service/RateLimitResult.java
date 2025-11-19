package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

/**
 * 限流结果
 * 用于封装限流检查的结果信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitResult {
    
    /**
     * 是否允许请求通过
     */
    private boolean allowed;
    
    /**
     * 限流阈值
     */
    private long limit;
    
    /**
     * 剩余可用次数
     */
    private long remaining;
    
    /**
     * 重置时间戳（毫秒）
     */
    private long resetTime;
    
    /**
     * 结果消息
     */
    private String message;
    
    /**
     * 客户端类型
     */
    @Builder.Default
    private String clientType = "unknown";
    
    /**
     * 限流类型
     */
    @Builder.Default
    private String limitType = "unknown";
    
    /**
     * 限流键值
     */
    @Builder.Default
    private String limitKey = "";
    
    /**
     * 限流算法类型
     */
    @Builder.Default
    private String algorithmType = "token_bucket";
    
    /**
     * 获取重置时间的剩余秒数
     */
    public long getResetTimeInSeconds() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = resetTime - currentTime;
        return timeDiff > 0 ? (timeDiff / 1000) : 0;
    }
    
    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= resetTime;
    }
    
    /**
     * 获取重置时间的友好格式
     */
    public String getResetTimeFormatted() {
        if (resetTime <= System.currentTimeMillis()) {
            return "已过期";
        }
        
        long seconds = getResetTimeInSeconds();
        if (seconds < 60) {
            return seconds + "秒后重置";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟后重置";
        } else {
            return (seconds / 3600) + "小时后重置";
        }
    }
    
    /**
     * 快速创建成功结果
     */
    public static RateLimitResult success(long limit, long remaining) {
        return RateLimitResult.builder()
            .allowed(true)
            .limit(limit)
            .remaining(remaining)
            .resetTime(System.currentTimeMillis() + 1000)
            .message("请求成功")
            .build();
    }
    
    /**
     * 快速创建失败结果
     */
    public static RateLimitResult failure(long limit, long remaining, String message) {
        return RateLimitResult.builder()
            .allowed(false)
            .limit(limit)
            .remaining(remaining)
            .resetTime(System.currentTimeMillis() + 1000)
            .message(message)
            .build();
    }
    
    /**
     * 创建基于当前时间的结果
     */
    public static RateLimitResult fromCurrentTime(boolean allowed, long limit, long remaining, String message) {
        return RateLimitResult.builder()
            .allowed(allowed)
            .limit(limit)
            .remaining(remaining)
            .resetTime(System.currentTimeMillis() + 1000)
            .message(message)
            .build();
    }
    
    /**
     * 转换为响应头信息
     */
    public void addHeaders(java.util.Map<String, String> headers) {
        headers.put("X-RateLimit-Limit", String.valueOf(limit));
        headers.put("X-RateLimit-Remaining", String.valueOf(remaining));
        headers.put("X-RateLimit-Reset", String.valueOf(resetTime / 1000)); // 秒为单位
        headers.put("X-RateLimit-Type", limitType);
        headers.put("X-RateLimit-Algorithm", algorithmType);
        
        if (!allowed) {
            headers.put("Retry-After", String.valueOf(getResetTimeInSeconds()));
        }
    }
    
    /**
     * 获取限流状态码
     */
    public int getStatusCode() {
        return allowed ? 200 : 429; // 429 = Too Many Requests
    }
    
    /**
     * 检查是否是严重的限流（达到硬性限制）
     */
    public boolean isHardLimit() {
        return !allowed && remaining == 0;
    }
    
    /**
     * 检查是否是软性限流（接近但未达到限制）
     */
    public boolean isSoftLimit() {
        return allowed && remaining < (limit * 0.1); // 剩余10%时认为是软性限流
    }
    
    /**
     * 获取限流严重程度
     */
    public String getSeverity() {
        if (!allowed) {
            return "BLOCKED";
        } else if (isSoftLimit()) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }
    
    /**
     * 获取限流类型的中文描述
     */
    public String getLimitTypeDescription() {
        switch (limitType) {
            case "ip":
                return "IP地址限流";
            case "user":
                return "用户限流";
            case "api":
                return "API限流";
            case "tenant":
                return "租户限流";
            default:
                return "未知限流";
        }
    }
    
    /**
     * 获取算法类型的中文描述
     */
    public String getAlgorithmDescription() {
        switch (algorithmType) {
            case "token_bucket":
                return "令牌桶算法";
            case "sliding_window":
                return "滑动窗口算法";
            case "fixed_window":
                return "固定窗口算法";
            default:
                return "未知算法";
        }
    }
}