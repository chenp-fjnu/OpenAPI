package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;

/**
 * 限流指标
 * 用于收集和统计限流相关的性能指标
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
public class RateLimitMetrics {
    
    /**
     * 总请求数
     */
    private long totalRequests;
    
    /**
     * 接受的请求数
     */
    private long acceptedRequests;
    
    /**
     * 拒绝的请求数
     */
    private long rejectedRequests;
    
    /**
     * 接受率百分比
     */
    private double acceptanceRate;
    
    /**
     * 拒绝率百分比
     */
    private double rejectionRate;
    
    /**
     * 当前使用率百分比
     */
    private double currentUsage;
    
    /**
     * 每秒请求数
     */
    private double requestsPerSecond;
    
    /**
     * 平均请求间隔（毫秒）
     */
    @Builder.Default
    private double averageRequestInterval = 0.0;
    
    /**
     * 峰值并发数
     */
    @Builder.Default
    private long peakConcurrency = 0;
    
    /**
     * 限流触发次数
     */
    @Builder.Default
    private long rateLimitTriggers = 0;
    
    /**
     * 平均处理时间（毫秒）
     */
    @Builder.Default
    private double averageProcessingTime = 0.0;
    
    /**
     * 最大处理时间（毫秒）
     */
    @Builder.Default
    private long maxProcessingTime = 0;
    
    /**
     * 最小处理时间（毫秒）
     */
    @Builder.Default
    private long minProcessingTime = Long.MAX_VALUE;
    
    /**
     * 监控时间窗口（秒）
     */
    @Builder.Default
    private long monitoringWindow = 60;
    
    /**
     * 测量时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
    
    /**
     * 计算接受率
     */
    public double calculateAcceptanceRate() {
        if (totalRequests == 0) {
            return 100.0;
        }
        return ((double) acceptedRequests / totalRequests) * 100.0;
    }
    
    /**
     * 计算拒绝率
     */
    public double calculateRejectionRate() {
        if (totalRequests == 0) {
            return 0.0;
        }
        return ((double) rejectedRequests / totalRequests) * 100.0;
    }
    
    /**
     * 计算每秒请求数
     */
    public double calculateRequestsPerSecond() {
        return (double) totalRequests / monitoringWindow;
    }
    
    /**
     * 检查是否需要关注
     */
    public boolean needsAttention() {
        return rejectionRate > 5.0 || currentUsage > 80.0 || peakConcurrency > totalRequests * 0.1;
    }
    
    /**
     * 获取健康状态
     */
    public String getHealthStatus() {
        if (rejectionRate > 10.0 || currentUsage > 90.0) {
            return "CRITICAL";
        } else if (rejectionRate > 5.0 || currentUsage > 80.0) {
            return "WARNING";
        } else if (currentUsage > 60.0) {
            return "CAUTION";
        } else {
            return "HEALTHY";
        }
    }
    
    /**
     * 获取处理时间级别
     */
    public String getProcessingTimeLevel() {
        double avgTime = averageProcessingTime;
        if (avgTime < 10) {
            return "EXCELLENT";
        } else if (avgTime < 50) {
            return "GOOD";
        } else if (avgTime < 100) {
            return "ACCEPTABLE";
        } else {
            return "POOR";
        }
    }
    
    /**
     * 生成性能报告
     */
    public String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 限流性能报告 ===\n");
        report.append("监控时间窗口: ").append(monitoringWindow).append("秒\n");
        report.append("总请求数: ").append(totalRequests).append("\n");
        report.append("接受请求: ").append(acceptedRequests)
              .append(" (").append(String.format("%.2f%%", calculateAcceptanceRate())).append(")\n");
        report.append("拒绝请求: ").append(rejectedRequests)
              .append(" (").append(String.format("%.2f%%", calculateRejectionRate())).append(")\n");
        report.append("平均请求率: ").append(String.format("%.2f", calculateRequestsPerSecond()))
              .append(" 请求/秒\n");
        report.append("当前使用率: ").append(String.format("%.1f%%", currentUsage)).append("\n");
        report.append("峰值并发: ").append(peakConcurrency).append("\n");
        report.append("限流触发: ").append(rateLimitTriggers).append("次\n");
        report.append("处理时间 - 平均: ").append(String.format("%.2f", averageProcessingTime))
              .append("ms, 最大: ").append(maxProcessingTime)
              .append("ms, 最小: ").append(minProcessingTime).append("ms\n");
        report.append("健康状态: ").append(getHealthStatus()).append("\n");
        report.append("性能级别: ").append(getProcessingTimeLevel()).append("\n");
        report.append("需要关注: ").append(needsAttention() ? "是" : "否").append("\n");
        
        return report.toString();
    }
    
    /**
     * 获取监控数据
     */
    public Map<String, Object> getMonitoringData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalRequests", totalRequests);
        data.put("acceptedRequests", acceptedRequests);
        data.put("rejectedRequests", rejectedRequests);
        data.put("acceptanceRate", calculateAcceptanceRate());
        data.put("rejectionRate", calculateRejectionRate());
        data.put("currentUsage", currentUsage);
        data.put("requestsPerSecond", calculateRequestsPerSecond());
        data.put("peakConcurrency", peakConcurrency);
        data.put("rateLimitTriggers", rateLimitTriggers);
        data.put("averageProcessingTime", averageProcessingTime);
        data.put("maxProcessingTime", maxProcessingTime);
        data.put("minProcessingTime", minProcessingTime == Long.MAX_VALUE ? 0 : minProcessingTime);
        data.put("monitoringWindow", monitoringWindow);
        data.put("timestamp", timestamp);
        data.put("healthStatus", getHealthStatus());
        data.put("processingTimeLevel", getProcessingTimeLevel());
        data.put("needsAttention", needsAttention());
        
        return data;
    }
    
    /**
     * 创建快照用于监控
     */
    public RateLimitSnapshot createSnapshot() {
        return RateLimitSnapshot.builder()
            .type("metrics")
            .identifier("system")
            .currentCount(acceptedRequests)
            .limit(totalRequests)
            .remaining(totalRequests - acceptedRequests)
            .usagePercentage(currentUsage)
            .algorithm("metric")
            .timestamp(LocalDateTime.now())
            .windowRemainingSeconds(monitoringWindow)
            .statusLevel(getHealthStatus())
            .needsAlert(needsAttention())
            .build();
    }
}