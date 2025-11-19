package com.group.gateway.routing.service;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Routing Statistics
 * 路由统计信息类
 */
@Data
public class RoutingStatistics {
    
    // 路由相关统计
    private final AtomicLong totalRoutes = new AtomicLong(0);
    private final AtomicLong activeRoutes = new AtomicLong(0);
    private final AtomicLong inactiveRoutes = new AtomicLong(0);
    
    // 路由选择统计
    private final LongAdder successfulSelections = new LongAdder();
    private final LongAdder failedSelections = new LongAdder();
    
    // 负载均衡统计
    private final LongAdder roundRobinSelections = new LongAdder();
    private final LongAdder randomSelections = new LongAdder();
    private final LongAdder leastConnectionsSelections = new LongAdder();
    private final LongAdder weightedResponseTimeSelections = new LongAdder();
    
    // 限流统计
    private final LongAdder rateLimitedRequests = new LongAdder();
    private final LongAdder allowedRequests = new LongAdder();
    
    // 熔断器统计
    private final LongAdder circuitBreakerOpened = new LongAdder();
    private final LongAdder circuitBreakerClosed = new LongAdder();
    private final LongAdder circuitBreakerHalfOpen = new LongAdder();
    
    // 健康检查统计
    private final AtomicLong healthyInstances = new AtomicLong(0);
    private final AtomicLong unhealthyInstances = new AtomicLong(0);
    private final AtomicLong totalHealthChecks = new AtomicLong(0);
    private final AtomicLong successfulHealthChecks = new AtomicLong(0);
    
    // 重试统计
    private final LongAdder retryAttempts = new LongAdder();
    private final LongAdder successfulRetries = new LongAdder();
    private final LongAdder failedRetries = new LongAdder();
    
    // 性能统计
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicLong responseTimeSamples = new AtomicLong(0);
    
    // 轮询索引
    private volatile int roundRobinIndex = 0;

    /**
     * 增加总路由数
     */
    public void incrementTotalRoutes() {
        totalRoutes.incrementAndGet();
        activeRoutes.incrementAndGet();
    }

    /**
     * 减少总路由数
     */
    public void decrementTotalRoutes() {
        totalRoutes.decrementAndGet();
        activeRoutes.decrementAndGet();
    }

    /**
     * 设置路由状态统计
     */
    public void updateRouteStatus(boolean active) {
        if (active) {
            activeRoutes.incrementAndGet();
            inactiveRoutes.decrementAndGet();
        } else {
            activeRoutes.decrementAndGet();
            inactiveRoutes.incrementAndGet();
        }
    }

    /**
     * 增加成功的路由选择
     */
    public void incrementSuccessfulSelections() {
        successfulSelections.increment();
    }

    /**
     * 增加失败的路由选择
     */
    public void incrementFailedSelections() {
        failedSelections.increment();
    }

    /**
     * 增加限流请求
     */
    public void incrementRateLimitedRequests() {
        rateLimitedRequests.increment();
    }

    /**
     * 增加允许的请求
     */
    public void incrementAllowedRequests() {
        allowedRequests.increment();
    }

    /**
     * 增加熔断器状态变化
     */
    public void incrementCircuitBreakerOpened() {
        circuitBreakerOpened.increment();
    }

    public void incrementCircuitBreakerClosed() {
        circuitBreakerClosed.increment();
    }

    public void incrementCircuitBreakerHalfOpen() {
        circuitBreakerHalfOpen.increment();
    }

    /**
     * 更新健康实例统计
     */
    public void updateHealthyInstanceCount(int healthy, int unhealthy) {
        healthyInstances.set(healthy);
        unhealthyInstances.set(unhealthy);
    }

    /**
     * 增加健康检查统计
     */
    public void incrementTotalHealthChecks() {
        totalHealthChecks.incrementAndGet();
    }

    public void incrementSuccessfulHealthChecks() {
        successfulHealthChecks.incrementAndGet();
    }

    /**
     * 增加重试统计
     */
    public void incrementRetryAttempts() {
        retryAttempts.increment();
    }

    public void incrementSuccessfulRetries() {
        successfulRetries.increment();
    }

    public void incrementFailedRetries() {
        failedRetries.increment();
    }

    /**
     * 记录响应时间
     */
    public void recordResponseTime(long responseTimeMs) {
        totalResponseTime.addAndGet(responseTimeMs);
        responseTimeSamples.incrementAndGet();
    }

    /**
     * 获取轮询索引
     */
    public int getNextRoundRobinIndex(int instanceCount) {
        if (instanceCount <= 0) {
            return 0;
        }
        
        int index = roundRobinIndex;
        roundRobinIndex = (roundRobinIndex + 1) % instanceCount;
        return index;
    }

    /**
     * 记录负载均衡算法使用情况
     */
    public void recordLoadBalancerUsage(String algorithm) {
        switch (algorithm.toUpperCase()) {
            case "ROUND_ROBIN":
                roundRobinSelections.increment();
                break;
            case "RANDOM":
                randomSelections.increment();
                break;
            case "LEAST_CONNECTIONS":
                leastConnectionsSelections.increment();
                break;
            case "WEIGHTED_RESPONSE_TIME":
                weightedResponseTimeSelections.increment();
                break;
        }
    }

    /**
     * 获取平均响应时间
     */
    public double getAverageResponseTime() {
        long samples = responseTimeSamples.get();
        if (samples == 0) {
            return 0.0;
        }
        return (double) totalResponseTime.get() / samples;
    }

    /**
     * 获取路由选择成功率
     */
    public double getSelectionSuccessRate() {
        long total = successfulSelections.sum() + failedSelections.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulSelections.sum() / total * 100;
    }

    /**
     * 获取健康检查成功率
     */
    public double getHealthCheckSuccessRate() {
        long total = totalHealthChecks.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulHealthChecks.get() / total * 100;
    }

    /**
     * 获取重试成功率
     */
    public double getRetrySuccessRate() {
        long total = successfulRetries.sum() + failedRetries.sum();
        if (total == 0) {
            return 0.0;
        }
        return (double) successfulRetries.sum() / total * 100;
    }

    /**
     * 重置所有统计
     */
    public void reset() {
        // 重置所有AtomicLong和LongAdder字段
        totalRoutes.set(0);
        activeRoutes.set(0);
        inactiveRoutes.set(0);
        
        // 重置选择统计
        successfulSelections.reset();
        failedSelections.reset();
        
        // 重置负载均衡统计
        roundRobinSelections.reset();
        randomSelections.reset();
        leastConnectionsSelections.reset();
        weightedResponseTimeSelections.reset();
        
        // 重置限流统计
        rateLimitedRequests.reset();
        allowedRequests.reset();
        
        // 重置熔断器统计
        circuitBreakerOpened.reset();
        circuitBreakerClosed.reset();
        circuitBreakerHalfOpen.reset();
        
        // 重置健康检查统计
        healthyInstances.set(0);
        unhealthyInstances.set(0);
        totalHealthChecks.set(0);
        successfulHealthChecks.set(0);
        
        // 重置重试统计
        retryAttempts.reset();
        successfulRetries.reset();
        failedRetries.reset();
        
        // 重置性能统计
        totalResponseTime.set(0);
        responseTimeSamples.set(0);
        
        // 重置轮询索引
        roundRobinIndex = 0;
    }

    /**
     * 获取统计摘要
     */
    public String getSummary() {
        return String.format(
            "RoutingStatistics{totalRoutes=%d, activeRoutes=%d, inactiveRoutes=%d, " +
            "successfulSelections=%d, failedSelections=%d, " +
            "rateLimitedRequests=%d, allowedRequests=%d, " +
            "healthyInstances=%d, unhealthyInstances=%d, " +
            "averageResponseTime=%.2fms, selectionSuccessRate=%.2f%%}",
            totalRoutes.get(), activeRoutes.get(), inactiveRoutes.get(),
            successfulSelections.sum(), failedSelections.sum(),
            rateLimitedRequests.sum(), allowedRequests.sum(),
            healthyInstances.get(), unhealthyInstances.get(),
            getAverageResponseTime(), getSelectionSuccessRate()
        );
    }
}