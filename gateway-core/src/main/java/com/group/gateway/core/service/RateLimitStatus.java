package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 限流状态
 * 用于记录和监控限流的当前状态信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitStatus {
    
    /**
     * 限流类型
     */
    private String type;
    
    /**
     * 限流标识符（IP、用户ID、租户ID等）
     */
    private String identifier;
    
    /**
     * 当前计数
     */
    private long currentCount;
    
    /**
     * 最大限制
     */
    private long limit;
    
    /**
     * 剩余可用次数
     */
    private long remaining;
    
    /**
     * 窗口大小（秒）
     */
    private long windowSize;
    
    /**
     * 窗口开始时间
     */
    private long windowStartTime;
    
    /**
     * 窗口结束时间
     */
    private long windowEndTime;
    
    /**
     * 当前使用的算法
     */
    private String algorithm;
    
    /**
     * 状态时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 百分比使用率
     */
    @Builder.Default
    private double usagePercentage = 0.0;
    
    /**
     * 是否接近限制
     */
    @Builder.Default
    private boolean approachingLimit = false;
    
    /**
     * 是否已达到限制
     */
    @Builder.Default
    private boolean limitReached = false;
    
    /**
     * 最后一次请求时间
     */
    @Builder.Default
    private long lastRequestTime = 0;
    
    /**
     * 累计请求次数
     */
    @Builder.Default
    private long totalRequests = 0;
    
    /**
     * 拒绝请求次数
     */
    @Builder.Default
    private long rejectedRequests = 0;
    
    /**
     * 错误信息
     */
    @Builder.Default
    private String errorMessage = "";
    
    /**
     * 初始化时间戳
     */
    public void initializeTimestamp() {
        this.timestamp = LocalDateTime.now();
        this.windowStartTime = System.currentTimeMillis();
        this.windowEndTime = this.windowStartTime + (windowSize * 1000);
        calculateUsagePercentage();
        updateLimitStatus();
    }
    
    /**
     * 更新时间戳和窗口信息
     */
    public void updateTimestamp() {
        this.timestamp = LocalDateTime.now();
        calculateUsagePercentage();
        updateLimitStatus();
    }
    
    /**
     * 计算使用率百分比
     */
    private void calculateUsagePercentage() {
        if (limit > 0) {
            this.usagePercentage = ((double) currentCount / limit) * 100.0;
        }
    }
    
    /**
     * 更新限制状态
     */
    private void updateLimitStatus() {
        this.approachingLimit = usagePercentage >= 80.0 && usagePercentage < 100.0;
        this.limitReached = currentCount >= limit;
    }
    
    /**
     * 记录请求
     */
    public void recordRequest(boolean allowed) {
        this.totalRequests++;
        this.lastRequestTime = System.currentTimeMillis();
        
        if (allowed) {
            this.currentCount++;
        } else {
            this.rejectedRequests++;
        }
        
        updateTimestamp();
    }
    
    /**
     * 重置计数器
     */
    public void reset() {
        this.currentCount = 0;
        this.usagePercentage = 0.0;
        this.approachingLimit = false;
        this.limitReached = false;
        this.windowStartTime = System.currentTimeMillis();
        this.windowEndTime = this.windowStartTime + (windowSize * 1000);
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 检查窗口是否过期
     */
    public boolean isWindowExpired() {
        return System.currentTimeMillis() >= windowEndTime;
    }
    
    /**
     * 获取窗口剩余时间（秒）
     */
    public long getWindowRemainingSeconds() {
        long remaining = windowEndTime - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * 获取状态级别的中文描述
     */
    public String getStatusLevelDescription() {
        if (limitReached) {
            return "已满载";
        } else if (approachingLimit) {
            return "接近限制";
        } else if (usagePercentage > 50.0) {
            return "中等使用";
        } else if (usagePercentage > 0) {
            return "正常使用";
        } else {
            return "空闲";
        }
    }
    
    /**
     * 获取状态级别的颜色标识
     */
    public String getStatusColor() {
        if (limitReached) {
            return "RED";
        } else if (approachingLimit) {
            return "YELLOW";
        } else if (usagePercentage > 50.0) {
            return "BLUE";
        } else {
            return "GREEN";
        }
    }
    
    /**
     * 获取使用率的进度条表示
     */
    public String getUsageProgressBar() {
        int barLength = 20;
        int filledLength = (int) (barLength * usagePercentage / 100.0);
        int emptyLength = barLength - filledLength;
        
        StringBuilder bar = new StringBuilder("[");
        
        if (limitReached) {
            bar.append("#".repeat(filledLength));
            bar.append("X".repeat(emptyLength));
        } else if (approachingLimit) {
            bar.append("#".repeat(filledLength));
            bar.append("-".repeat(emptyLength));
        } else {
            bar.append("=".repeat(filledLength));
            bar.append("-".repeat(emptyLength));
        }
        
        bar.append("] ");
        bar.append(String.format("%.1f%%", usagePercentage));
        
        return bar.toString();
    }
    
    /**
     * 获取时间格式化的窗口信息
     */
    public String getWindowInfo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String startTime = LocalDateTime.ofEpochSecond(windowStartTime / 1000, 0, 
            java.time.ZoneOffset.UTC).format(formatter);
        String endTime = LocalDateTime.ofEpochSecond(windowEndTime / 1000, 0, 
            java.time.ZoneOffset.UTC).format(formatter);
        
        return String.format("%s - %s (%d秒)", startTime, endTime, getWindowRemainingSeconds());
    }
    
    /**
     * 获取性能指标
     */
    public RateLimitMetrics getMetrics() {
        double rejectionRate = totalRequests > 0 ? 
            (double) rejectedRequests / totalRequests * 100.0 : 0.0;
        
        return RateLimitMetrics.builder()
            .totalRequests(totalRequests)
            .rejectedRequests(rejectedRequests)
            .acceptanceRate(100.0 - rejectionRate)
            .rejectionRate(rejectionRate)
            .currentUsage(usagePercentage)
            .requestsPerSecond(windowSize > 0 ? 
                (double) currentCount / windowSize : 0.0)
            .build();
    }
    
    /**
     * 检查是否需要预警
     */
    public boolean needsAlert() {
        return limitReached || (approachingLimit && rejectedRequests > 0);
    }
    
    /**
     * 创建快照用于监控
     */
    public RateLimitSnapshot createSnapshot() {
        return RateLimitSnapshot.builder()
            .type(type)
            .identifier(identifier)
            .currentCount(currentCount)
            .limit(limit)
            .remaining(remaining)
            .usagePercentage(usagePercentage)
            .approachingLimit(approachingLimit)
            .limitReached(limitReached)
            .algorithm(algorithm)
            .timestamp(timestamp)
            .windowRemainingSeconds(getWindowRemainingSeconds())
            .statusLevel(getStatusLevelDescription())
            .needsAlert(needsAlert())
            .build();
    }
    
    /**
     * 生成状态报告
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 限流状态报告 ===\n");
        report.append("类型: ").append(type).append("\n");
        report.append("标识符: ").append(identifier).append("\n");
        report.append("当前计数: ").append(currentCount).append("/").append(limit).append("\n");
        report.append("使用率: ").append(String.format("%.1f%%", usagePercentage)).append("\n");
        report.append("状态: ").append(getStatusLevelDescription()).append("\n");
        report.append("窗口: ").append(getWindowInfo()).append("\n");
        report.append("算法: ").append(algorithm).append("\n");
        report.append("总请求: ").append(totalRequests).append("\n");
        report.append("拒绝请求: ").append(rejectedRequests).append("\n");
        report.append("接受率: ").append(String.format("%.2f%%", getMetrics().getAcceptanceRate())).append("\n");
        report.append("进度: ").append(getUsageProgressBar()).append("\n");
        report.append("时间: ").append(timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        if (needsAlert()) {
            report.append("\n⚠️  需要关注: ").append(getAlertMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * 获取预警消息
     */
    public String getAlertMessage() {
        if (limitReached) {
            return "限流已达到上限，建议检查流量激增原因";
        } else if (approachingLimit && rejectedRequests > 0) {
            return "接近限流上限且存在拒绝请求，建议调整限流参数";
        }
        return "正常";
    }
}