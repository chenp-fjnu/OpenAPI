package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 限流快照
 * 用于临时记录和展示限流状态的简化版本
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitSnapshot {
    
    /**
     * 限流类型
     */
    private String type;
    
    /**
     * 标识符（IP、用户ID、租户ID等）
     */
    private String identifier;
    
    /**
     * 当前计数
     */
    @Builder.Default
    private long currentCount = 0;
    
    /**
     * 限制值
     */
    @Builder.Default
    private long limit = 100;
    
    /**
     * 剩余可用数量
     */
    @Builder.Default
    private long remaining = 100;
    
    /**
     * 使用百分比
     */
    @Builder.Default
    private double usagePercentage = 0.0;
    
    /**
     * 使用的算法类型
     */
    @Builder.Default
    private String algorithm = "token_bucket";
    
    /**
     * 快照时间戳
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * 窗口剩余时间（秒）
     */
    @Builder.Default
    private long windowRemainingSeconds = 60;
    
    /**
     * 状态级别
     */
    @Builder.Default
    private String statusLevel = "HEALTHY";
    
    /**
     * 是否需要告警
     */
    @Builder.Default
    private boolean needsAlert = false;
    
    /**
     * 重置时间
     */
    @Builder.Default
    private LocalDateTime resetTime = LocalDateTime.now().plusSeconds(60);
    
    /**
     * 限流触发次数
     */
    @Builder.Default
    private long triggerCount = 0;
    
    /**
     * 恢复策略
     */
    @Builder.Default
    private String recoveryStrategy = "wait";
    
    /**
     * 检查是否可以继续请求
     */
    public boolean canContinue() {
        return currentCount < limit;
    }
    
    /**
     * 检查是否接近限制
     */
    public boolean isNearLimit() {
        return remaining <= limit * 0.1; // 剩余10%视为接近限制
    }
    
    /**
     * 检查是否需要紧急告警
     */
    public boolean needsCriticalAlert() {
        return usagePercentage > 95.0 || remaining <= 1;
    }
    
    /**
     * 获取状态颜色
     */
    public String getStatusColor() {
        return switch (statusLevel) {
            case "CRITICAL" -> "#FF0000";
            case "WARNING" -> "#FF8000";
            case "CAUTION" -> "#FFFF00";
            default -> "#00FF00";
        };
    }
    
    /**
     * 获取使用率描述
     */
    public String getUsageDescription() {
        double usage = usagePercentage;
        if (usage >= 90.0) {
            return "极高使用率";
        } else if (usage >= 70.0) {
            return "高使用率";
        } else if (usage >= 50.0) {
            return "中等使用率";
        } else if (usage >= 25.0) {
            return "正常使用率";
        } else {
            return "低使用率";
        }
    }
    
    /**
     * 获取剩余时间描述
     */
    public String getRemainingTimeDescription() {
        long seconds = windowRemainingSeconds;
        if (seconds >= 3600) {
            return String.format("%.1f小时", seconds / 3600.0);
        } else if (seconds >= 60) {
            return String.format("%d分钟", seconds / 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }
    
    /**
     * 生成状态描述
     */
    public String generateStatusDescription() {
        StringBuilder description = new StringBuilder();
        description.append("限流类型: ").append(type).append(", ");
        description.append("标识符: ").append(identifier).append(", ");
        description.append("使用率: ").append(String.format("%.1f%%", usagePercentage)).append(", ");
        description.append("剩余: ").append(remaining).append("/").append(limit);
        
        if (needsAlert) {
            description.append(", 需要关注: 是");
        }
        if (needsCriticalAlert()) {
            description.append(", 紧急告警: 是");
        }
        
        return description.toString();
    }
    
    /**
     * 转换为监控数据
     */
    public Map<String, Object> toMonitoringData() {
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("identifier", identifier);
        data.put("currentCount", currentCount);
        data.put("limit", limit);
        data.put("remaining", remaining);
        data.put("usagePercentage", usagePercentage);
        data.put("algorithm", algorithm);
        data.put("timestamp", timestamp);
        data.put("windowRemainingSeconds", windowRemainingSeconds);
        data.put("statusLevel", statusLevel);
        data.put("needsAlert", needsAlert);
        data.put("resetTime", resetTime);
        data.put("triggerCount", triggerCount);
        data.put("recoveryStrategy", recoveryStrategy);
        data.put("canContinue", canContinue());
        data.put("isNearLimit", isNearLimit());
        data.put("needsCriticalAlert", needsCriticalAlert());
        data.put("statusColor", getStatusColor());
        data.put("usageDescription", getUsageDescription());
        data.put("remainingTimeDescription", getRemainingTimeDescription());
        
        return data;
    }
    
    /**
     * 创建简化版本
     */
    public RateLimitSnapshot createSimplifiedSnapshot() {
        return RateLimitSnapshot.builder()
            .type("simplified")
            .identifier("system")
            .limit(100)
            .remaining(Math.max(0, 100 - (int) (usagePercentage)))
            .usagePercentage(usagePercentage)
            .statusLevel(statusLevel)
            .needsAlert(needsAlert)
            .timestamp(LocalDateTime.now())
            .build();
    }
}