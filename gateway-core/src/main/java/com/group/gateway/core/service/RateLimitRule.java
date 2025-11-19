package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

import java.util.regex.Pattern;

/**
 * 限流规则
 * 用于定义具体的限流规则参数
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitRule {
    
    /**
     * 是否启用限流
     */
    private boolean enableRateLimit;
    
    /**
     * 限流阈值
     */
    private long limit;
    
    /**
     * 窗口大小（秒）
     */
    private long windowSize;
    
    /**
     * 限流算法：token_bucket, sliding_window, fixed_window
     */
    private String algorithm;
    
    /**
     * 优先级（数字越小优先级越高）
     */
    @Builder.Default
    private int priority = 10;
    
    /**
     * 路径模式
     */
    @Builder.Default
    private String pathPattern = "/*";
    
    /**
     * HTTP方法限制
     */
    private String[] methods;
    
    /**
     * 客户端类型限制
     */
    private String[] clientTypes;
    
    /**
     * 租户限制
     */
    private String[] tenantIds;
    
    /**
     * 用户组限制
     */
    private String[] userGroups;
    
    /**
     * 时间范围（cron表达式）
     */
    private String timeRange;
    
    /**
     * 限流消息
     */
    @Builder.Default
    private String message = "请求过于频繁，请稍后再试";
    
    /**
     * 是否启用突发流量处理
     */
    @Builder.Default
    private boolean enableBurst = true;
    
    /**
     * 突发流量倍数
     */
    @Builder.Default
    private double burstMultiplier = 2.0;
    
    /**
     * 预热时间（秒）
     */
    @Builder.Default
    private long warmupTime = 60;
    
    /**
     * 恢复策略
     */
    @Builder.Default
    private RecoveryStrategy recoveryStrategy = RecoveryStrategy.IMMIDIATE;
    
    /**
     * 检查规则是否匹配给定路径
     */
    public boolean matches(String path, String method) {
        // 检查路径模式
        if (!matchesPath(path)) {
            return false;
        }
        
        // 检查HTTP方法
        if (methods != null && methods.length > 0) {
            boolean methodMatched = false;
            for (String allowedMethod : methods) {
                if (allowedMethod.equalsIgnoreCase(method)) {
                    methodMatched = true;
                    break;
                }
            }
            if (!methodMatched) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 检查路径是否匹配规则
     */
    private boolean matchesPath(String path) {
        if (pathPattern.equals("/*") || pathPattern.equals("/**")) {
            return true;
        }
        
        if (pathPattern.contains("*")) {
            String regex = pathPattern.replace("*", ".*");
            return Pattern.matches(regex, path);
        } else {
            return pathPattern.equals(path);
        }
    }
    
    /**
     * 检查是否在指定时间范围内
     */
    public boolean isWithinTimeRange() {
        if (timeRange == null || timeRange.isEmpty()) {
            return true; // 不限制时间
        }
        
        // 这里可以实现基于cron表达式的时间范围检查
        // 简化实现，返回true
        return true;
    }
    
    /**
     * 获取实际的限流阈值（考虑突发流量）
     */
    public long getEffectiveLimit(double currentLoadFactor) {
        if (!enableBurst || burstMultiplier <= 1.0) {
            return limit;
        }
        
        // 当系统负载较低时，允许突发流量
        if (currentLoadFactor < 0.5) {
            return (long) (limit * burstMultiplier);
        }
        
        return limit;
    }
    
    /**
     * 获取预热完成时间
     */
    public long getWarmupCompleteTime() {
        return System.currentTimeMillis() + (warmupTime * 1000);
    }
    
    /**
     * 检查预热是否完成
     */
    public boolean isWarmupComplete() {
        return System.currentTimeMillis() >= getWarmupCompleteTime();
    }
    
    /**
     * 获取算法的中文描述
     */
    public String getAlgorithmDescription() {
        switch (algorithm != null ? algorithm.toLowerCase() : "token_bucket") {
            case "token_bucket":
                return "令牌桶算法";
            case "sliding_window":
                return "滑动窗口算法";
            case "fixed_window":
                return "固定窗口算法";
            case "leaky_bucket":
                return "漏桶算法";
            default:
                return "未知算法";
        }
    }
    
    /**
     * 获取恢复策略的中文描述
     */
    public String getRecoveryStrategyDescription() {
        switch (recoveryStrategy) {
            case IMMIDIATE:
                return "立即恢复";
            case GRADUAL:
                return "渐进恢复";
            case MANUAL:
                return "手动恢复";
            default:
                return "未知策略";
        }
    }
    
    /**
     * 创建适用于API的规则
     */
    public static RateLimitRule forApi(String apiPath, long limit, String algorithm) {
        return RateLimitRule.builder()
            .enableRateLimit(true)
            .limit(limit)
            .windowSize(60)
            .algorithm(algorithm)
            .pathPattern(apiPath)
            .priority(1)
            .message("API访问过于频繁，请稍后再试")
            .enableBurst(true)
            .burstMultiplier(1.5)
            .build();
    }
    
    /**
     * 创建适用于用户的规则
     */
    public static RateLimitRule forUser(long limit, String[] userGroups) {
        return RateLimitRule.builder()
            .enableRateLimit(true)
            .limit(limit)
            .windowSize(60)
            .algorithm("sliding_window")
            .pathPattern("/*")
            .userGroups(userGroups)
            .priority(5)
            .message("用户请求过于频繁，请稍后再试")
            .enableBurst(true)
            .burstMultiplier(2.0)
            .build();
    }
    
    /**
     * 创建适用于租户的规则
     */
    public static RateLimitRule forTenant(String tenantId, long limit) {
        return RateLimitRule.builder()
            .enableRateLimit(true)
            .limit(limit)
            .windowSize(60)
            .algorithm("token_bucket")
            .pathPattern("/*")
            .tenantIds(new String[]{tenantId})
            .priority(3)
            .message("租户请求过于频繁，请稍后再试")
            .enableBurst(true)
            .burstMultiplier(3.0)
            .build();
    }
    
    /**
     * 创建适用于IP的规则
     */
    public static RateLimitRule forIp(long limit) {
        return RateLimitRule.builder()
            .enableRateLimit(true)
            .limit(limit)
            .windowSize(60)
            .algorithm("fixed_window")
            .pathPattern("/*")
            .priority(8)
            .message("IP地址请求过于频繁，请稍后再试")
            .enableBurst(false)
            .build();
    }
    
    /**
     * 创建白名单规则（不限流）
     */
    public static RateLimitRule whitelist(String pathPattern) {
        return RateLimitRule.builder()
            .enableRateLimit(false)
            .limit(Long.MAX_VALUE)
            .windowSize(60)
            .algorithm("token_bucket")
            .pathPattern(pathPattern)
            .priority(0)
            .message("白名单路径，无需限流")
            .build();
    }
    
    /**
     * 恢复策略枚举
     */
    public enum RecoveryStrategy {
        IMMIDIATE,    // 立即恢复
        GRADUAL,      // 渐进恢复
        MANUAL        // 手动恢复
    }
    
    /**
     * 限流算法类型
     */
    public enum AlgorithmType {
        TOKEN_BUCKET("token_bucket"),
        SLIDING_WINDOW("sliding_window"),
        FIXED_WINDOW("fixed_window"),
        LEAKY_BUCKET("leaky_bucket");
        
        private final String value;
        
        AlgorithmType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    /**
     * 优先级级别
     */
    public static class Priority {
        public static final int CRITICAL = 1;
        public static final int HIGH = 3;
        public static final int NORMAL = 5;
        public static final int LOW = 8;
        public static final int LOWEST = 10;
    }
}