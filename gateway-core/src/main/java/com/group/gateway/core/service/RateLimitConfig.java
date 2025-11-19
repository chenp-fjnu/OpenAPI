package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * 限流配置
 * 用于定义和管理限流规则配置
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitConfig {
    
    /**
     * 是否启用限流
     */
    private boolean enableRateLimit;
    
    /**
     * IP限流（每秒请求数）
     */
    private long ipLimit;
    
    /**
     * 用户限流（每秒请求数）
     */
    private long userLimit;
    
    /**
     * API限流（每秒请求数）
     */
    private long apiLimit;
    
    /**
     * 租户限流（每秒请求数）
     */
    private long tenantLimit;
    
    /**
     * 全局限流（每秒请求数）
     */
    private long globalLimit;
    
    /**
     * 令牌桶容量
     */
    private long tokenBucketCapacity;
    
    /**
     * 令牌桶补充速率（每秒）
     */
    private long tokenBucketRefillRate;
    
    /**
     * 滑动窗口大小（秒）
     */
    private long slidingWindowSize;
    
    /**
     * 预定义路径规则（白名单）
     */
    private List<String> whitelistPaths;
    
    /**
     * 需要严格限流的路径模式
     */
    private List<String> strictLimitPatterns;
    
    /**
     * 高优先级限流规则
     */
    private ConcurrentMap<String, RateLimitRule> highPriorityRules;
    
    /**
     * 默认限流规则
     */
    @Builder.Default
    private RateLimitRule defaultRule = RateLimitRule.builder()
        .enableRateLimit(true)
        .limit(100)
        .windowSize(60)
        .algorithm("token_bucket")
        .build();
    
    /**
     * 检查路径是否在白名单中
     */
    public boolean isWhitelisted(String path) {
        if (whitelistPaths == null || whitelistPaths.isEmpty()) {
            return false;
        }
        
        return whitelistPaths.stream()
            .anyMatch(pattern -> {
                if (pattern.contains("*")) {
                    // 处理通配符
                    String regex = pattern.replace("*", ".*");
                    return Pattern.matches(regex, path);
                } else {
                    return pattern.equals(path);
                }
            });
    }
    
    /**
     * 检查路径是否需要严格限流
     */
    public boolean isStrictLimitRequired(String path) {
        if (strictLimitPatterns == null || strictLimitPatterns.isEmpty()) {
            return false;
        }
        
        return strictLimitPatterns.stream()
            .anyMatch(pattern -> {
                if (pattern.contains("*")) {
                    String regex = pattern.replace("*", ".*");
                    return Pattern.matches(regex, path);
                } else {
                    return pattern.equals(path);
                }
            });
    }
    
    /**
     * 获取高优先级规则
     */
    public RateLimitRule getHighPriorityRule(String path) {
        if (highPriorityRules == null || highPriorityRules.isEmpty()) {
            return null;
        }
        
        return highPriorityRules.values().stream()
            .filter(rule -> {
                if (rule.getPathPattern().contains("*")) {
                    String regex = rule.getPathPattern().replace("*", ".*");
                    return Pattern.matches(regex, path);
                } else {
                    return rule.getPathPattern().equals(path);
                }
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 添加高优先级规则
     */
    public void addHighPriorityRule(String name, RateLimitRule rule) {
        if (highPriorityRules == null) {
            highPriorityRules = new ConcurrentHashMap<>();
        }
        highPriorityRules.put(name, rule);
    }
    
    /**
     * 移除高优先级规则
     */
    public void removeHighPriorityRule(String name) {
        if (highPriorityRules != null) {
            highPriorityRules.remove(name);
        }
    }
    
    /**
     * 获取适用的限流规则
     */
    public RateLimitRule getApplicableRule(String path, String method) {
        // 1. 优先检查白名单
        if (isWhitelisted(path)) {
            return RateLimitRule.builder()
                .enableRateLimit(false)
                .limit(Long.MAX_VALUE)
                .build();
        }
        
        // 2. 检查高优先级规则
        RateLimitRule highPriorityRule = getHighPriorityRule(path);
        if (highPriorityRule != null && highPriorityRule.isEnableRateLimit()) {
            return highPriorityRule;
        }
        
        // 3. 检查是否需要严格限流
        if (isStrictLimitRequired(path)) {
            return RateLimitRule.builder()
                .enableRateLimit(true)
                .limit(50) // 严格限制
                .windowSize(60)
                .algorithm("sliding_window")
                .priority(1)
                .build();
        }
        
        // 4. 返回默认规则
        return defaultRule;
    }
    
    /**
     * 克隆配置并应用新的限流值
     */
    public RateLimitConfig withLimits(long newLimit) {
        return RateLimitConfig.builder()
            .enableRateLimit(this.enableRateLimit)
            .ipLimit(newLimit)
            .userLimit(newLimit)
            .apiLimit(newLimit)
            .tenantLimit(newLimit)
            .globalLimit(newLimit)
            .tokenBucketCapacity(this.tokenBucketCapacity)
            .tokenBucketRefillRate(this.tokenBucketRefillRate)
            .slidingWindowSize(this.slidingWindowSize)
            .whitelistPaths(this.whitelistPaths)
            .strictLimitPatterns(this.strictLimitPatterns)
            .highPriorityRules(this.highPriorityRules)
            .defaultRule(this.defaultRule)
            .build();
    }
    
    /**
     * 创建适合当前环境的配置
     */
    public static RateLimitConfig createForEnvironment(String environment) {
        switch (environment.toLowerCase()) {
            case "production":
                return RateLimitConfig.builder()
                    .enableRateLimit(true)
                    .ipLimit(100)
                    .userLimit(1000)
                    .apiLimit(200)
                    .tenantLimit(10000)
                    .globalLimit(100000)
                    .tokenBucketCapacity(1000)
                    .tokenBucketRefillRate(100)
                    .slidingWindowSize(60)
                    .build();
                    
            case "staging":
                return RateLimitConfig.builder()
                    .enableRateLimit(true)
                    .ipLimit(500)
                    .userLimit(5000)
                    .apiLimit(1000)
                    .tenantLimit(50000)
                    .globalLimit(500000)
                    .tokenBucketCapacity(5000)
                    .tokenBucketRefillRate(500)
                    .slidingWindowSize(60)
                    .build();
                    
            case "development":
                return RateLimitConfig.builder()
                    .enableRateLimit(false)
                    .ipLimit(10000)
                    .userLimit(100000)
                    .apiLimit(20000)
                    .tenantLimit(1000000)
                    .globalLimit(10000000)
                    .tokenBucketCapacity(100000)
                    .tokenBucketRefillRate(10000)
                    .slidingWindowSize(60)
                    .build();
                    
            default:
                return RateLimitConfig.builder()
                    .enableRateLimit(true)
                    .ipLimit(100)
                    .userLimit(1000)
                    .apiLimit(200)
                    .tenantLimit(10000)
                    .globalLimit(100000)
                    .tokenBucketCapacity(1000)
                    .tokenBucketRefillRate(100)
                    .slidingWindowSize(60)
                    .build();
        }
    }
}