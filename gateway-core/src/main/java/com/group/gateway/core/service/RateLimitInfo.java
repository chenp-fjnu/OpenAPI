package com.group.gateway.core.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * 限流信息
 * 用于记录请求的限流相关信息，包括限流配置、当前状态、统计数据等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitInfo {
    
    /**
     * 限流类型
     */
    private LimitType limitType;
    
    /**
     * 限流维度键（如IP地址、用户ID、API路径等）
     */
    private String limitKey;
    
    /**
     * 限流阈值
     */
    private Integer threshold;
    
    /**
     * 时间窗口（秒）
     */
    private Integer timeWindow;
    
    /**
     * 当前请求数
     */
    private Integer currentCount;
    
    /**
     * 重置时间
     */
    private Instant resetTime;
    
    /**
     * 剩余配额
     */
    private Integer remainingQuota;
    
    /**
     * 是否被限流
     */
    private boolean limited;
    
    /**
     * 限流原因
     */
    private String limitReason;
    
    /**
     * 限流算法类型
     */
    private AlgorithmType algorithmType;
    
    /**
     * 请求优先级
     */
    private Priority priority;
    
    /**
     * 是否为突发请求
     */
    private boolean burstRequest;
    
    /**
     * 限流策略
     */
    private LimitStrategy limitStrategy;
    
    /**
     * 违规次数
     */
    private Integer violationCount;
    
    /**
     * 最后违规时间
     */
    private Instant lastViolationTime;
    
    /**
     * 白名单标记
     */
    private boolean whitelist;
    
    /**
     * 黑名单标记
     */
    private boolean blacklist;
    
    /**
     * 获取限流描述
     */
    public String getLimitDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append(limitType != null ? limitType.getDisplayName() : "unknown");
        desc.append(" (");
        desc.append(currentCount != null ? currentCount : 0);
        desc.append("/");
        desc.append(threshold != null ? threshold : 0);
        desc.append(")");
        return desc.toString();
    }
    
    /**
     * 获取限流百分比
     */
    public double getLimitPercentage() {
        if (threshold == null || threshold == 0 || currentCount == null) {
            return 0.0;
        }
        return (double) currentCount / threshold * 100;
    }
    
    /**
     * 检查是否接近限流阈值
     */
    public boolean isNearLimit() {
        return getLimitPercentage() >= 80.0; // 80%以上认为接近限流
    }
    
    /**
     * 检查是否严重超限
     */
    public boolean isSeverelyOverLimit() {
        return getLimitPercentage() >= 150.0; // 150%以上认为严重超限
    }
    
    /**
     * 获取剩余时间（秒）
     */
    public long getRemainingTimeSeconds() {
        if (resetTime == null) {
            return 0;
        }
        long remainingMillis = resetTime.toEpochMilli() - System.currentTimeMillis();
        return Math.max(0, remainingMillis / 1000);
    }
    
    /**
     * 记录违规
     */
    public void recordViolation() {
        this.violationCount = (violationCount != null ? violationCount : 0) + 1;
        this.lastViolationTime = Instant.now();
    }
    
    /**
     * 限流类型枚举
     */
    public enum LimitType {
        IP("IP地址限流"),
        USER("用户限流"),
        API("API限流"),
        TENANT("租户限流"),
        GLOBAL("全局限流"),
        CUSTOM("自定义限流");
        
        private final String displayName;
        
        LimitType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 算法类型枚举
     */
    public enum AlgorithmType {
        TOKEN_BUCKET("令牌桶"),
        SLIDING_WINDOW("滑动窗口"),
        FIXED_WINDOW("固定窗口"),
        LEAKY_BUCKET("漏桶");
        
        private final String displayName;
        
        AlgorithmType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW("低优先级"),
        NORMAL("普通优先级"),
        HIGH("高优先级"),
        CRITICAL("关键优先级");
        
        private final String displayName;
        
        Priority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * 限流策略枚举
     */
    public enum LimitStrategy {
        BLOCK("直接拒绝"),
        QUEUE("排队等待"),
        GRADUAL("逐步限制"),
        ADAPTIVE("自适应限流");
        
        private final String displayName;
        
        LimitStrategy(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    @Override
    public String toString() {
        return "RateLimitInfo{" +
                "limitType=" + limitType +
                ", limitKey='" + limitKey + '\'' +
                ", currentCount=" + currentCount +
                ", threshold=" + threshold +
                ", limited=" + limited +
                ", remainingQuota=" + remainingQuota +
                ", violationCount=" + violationCount +
                '}';
    }
}