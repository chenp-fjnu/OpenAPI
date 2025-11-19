package com.group.gateway.core.controller;

import com.group.gateway.common.response.Result;
import com.group.gateway.core.service.RateLimitMetrics;
import com.group.gateway.core.service.RequestTraceService;
import com.group.gateway.core.service.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控指标控制器
 * 提供系统性能指标、限流统计和系统状态信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final RequestTraceService requestTraceService;
    private final RateLimitMetrics rateLimitMetrics;
    private final ConcurrentHashMap<String, Object> cacheMetrics = new ConcurrentHashMap<>();
    
    /**
     * 获取系统总览指标
     */
    @GetMapping("/overview")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> getOverviewMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // 时间戳
            metrics.put("timestamp", LocalDateTime.now());
            metrics.put("collectTime", System.currentTimeMillis());
            
            // 系统基本信息
            Map<String, Object> system = new HashMap<>();
            system.put("service", "gateway-core");
            system.put("version", "1.0.0");
            system.put("environment", System.getProperty("spring.profiles.active", "default"));
            system.put("javaVersion", System.getProperty("java.version"));
            system.put("osName", System.getProperty("os.name"));
            metrics.put("system", system);
            
            // 限流指标
            Map<String, Object> rateLimit = new HashMap<>();
            rateLimit.put("totalRequests", rateLimitMetrics.getTotalRequests());
            rateLimit.put("acceptedRequests", rateLimitMetrics.getAcceptedRequests());
            rateLimit.put("rejectedRequests", rateLimitMetrics.getRejectedRequests());
            rateLimit.put("acceptanceRate", rateLimitMetrics.getAcceptanceRate());
            rateLimit.put("rejectionRate", rateLimitMetrics.getRejectionRate());
            rateLimit.put("rps", rateLimitMetrics.getRps());
            metrics.put("rateLimit", rateLimit);
            
            // 请求追踪指标
            Map<String, Object> tracing = new HashMap<>();
            tracing.put("totalTraces", requestTraceService.getTotalTraces());
            tracing.put("activeTraces", requestTraceService.getActiveTraces());
            tracing.put("averageResponseTime", requestTraceService.getAverageResponseTime());
            tracing.put("p95ResponseTime", requestTraceService.getP95ResponseTime());
            tracing.put("errorRate", requestTraceService.getErrorRate());
            metrics.put("tracing", tracing);
            
            // 缓存指标
            Map<String, Object> cache = getCacheMetrics();
            metrics.put("cache", cache);
            
            return ResponseEntity.ok(Result.success(metrics));
        });
    }
    
    /**
     * 获取限流详细指标
     */
    @GetMapping("/rate-limit")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> getRateLimitMetrics(
            @RequestParam(defaultValue = "false") boolean includeHistory) {
        
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // 基础指标
            metrics.put("totalRequests", rateLimitMetrics.getTotalRequests());
            metrics.put("acceptedRequests", rateLimitMetrics.getAcceptedRequests());
            metrics.put("rejectedRequests", rateLimitMetrics.getRejectedRequests());
            metrics.put("acceptanceRate", rateLimitMetrics.getAcceptanceRate());
            metrics.put("rejectionRate", rateLimitMetrics.getRejectionRate());
            metrics.put("rps", rateLimitMetrics.getRps());
            
            // 当前限流规则统计
            Map<String, Object> rules = getRateLimitRulesStats();
            metrics.put("rules", rules);
            
            // 限流状态统计
            Map<String, Object> status = getRateLimitStatusStats();
            metrics.put("status", status);
            
            // 如果需要历史数据
            if (includeHistory) {
                Map<String, Object> history = getRateLimitHistory();
                metrics.put("history", history);
            }
            
            metrics.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(Result.success(metrics));
        });
    }
    
    /**
     * 获取性能指标
     */
    @GetMapping("/performance")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> getPerformanceMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // 响应时间统计
            Map<String, Object> responseTime = new HashMap<>();
            responseTime.put("average", requestTraceService.getAverageResponseTime());
            responseTime.put("p50", requestTraceService.getP50ResponseTime());
            responseTime.put("p90", requestTraceService.getP90ResponseTime());
            responseTime.put("p95", requestTraceService.getP95ResponseTime());
            responseTime.put("p99", requestTraceService.getP99ResponseTime());
            responseTime.put("max", requestTraceService.getMaxResponseTime());
            metrics.put("responseTime", responseTime);
            
            // 错误率统计
            Map<String, Object> errors = new HashMap<>();
            errors.put("total", requestTraceService.getTotalErrors());
            errors.put("rate", requestTraceService.getErrorRate());
            errors.put("timeouts", requestTraceService.getTimeoutCount());
            errors.put("circuitBreakerTrips", getCircuitBreakerTripCount());
            metrics.put("errors", errors);
            
            // 吞吐量统计
            Map<String, Object> throughput = new HashMap<>();
            throughput.put("currentRps", rateLimitMetrics.getRps());
            throughput.put("peakRps", getPeakRps());
            throughput.put("averageRps", getAverageRps());
            throughput.put("totalRequests", rateLimitMetrics.getTotalRequests());
            metrics.put("throughput", throughput);
            
            metrics.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(Result.success(metrics));
        });
    }
    
    /**
     * 获取系统资源指标
     */
    @GetMapping("/system")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> getSystemMetrics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> metrics = new HashMap<>();
            
            // JVM指标
            Map<String, Object> jvm = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();
            jvm.put("totalMemory", runtime.totalMemory());
            jvm.put("freeMemory", runtime.freeMemory());
            jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            jvm.put("maxMemory", runtime.maxMemory());
            jvm.put("availableProcessors", runtime.availableProcessors());
            metrics.put("jvm", jvm);
            
            // 系统指标
            Map<String, Object> system = new HashMap<>();
            system.put("loadAverage", getSystemLoadAverage());
            system.put("uptime", System.currentTimeMillis());
            system.put("timestamp", LocalDateTime.now());
            metrics.put("system", system);
            
            return ResponseEntity.ok(Result.success(metrics));
        });
    }
    
    /**
     * 重置指标统计
     */
    @PostMapping("/reset")
    public Mono<ResponseEntity<Result<String>>> resetMetrics() {
        return Mono.fromCallable(() -> {
            rateLimitMetrics.reset();
            requestTraceService.clearTraces();
            cacheMetrics.clear();
            
            log.info("指标统计已重置");
            return ResponseEntity.ok(Result.success("指标统计已重置"));
        });
    }
    
    /**
     * 获取缓存指标
     */
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cache = new HashMap<>();
        cache.put("cacheMetrics", cacheMetrics.size());
        cache.put("keys", new ArrayList<>(cacheMetrics.keySet()));
        return cache;
    }
    
    /**
     * 获取限流规则统计
     */
    private Map<String, Object> getRateLimitRulesStats() {
        Map<String, Object> rules = new HashMap<>();
        // 这里需要根据实际的RateLimitService实现来获取规则统计
        rules.put("totalRules", 0);
        rules.put("activeRules", 0);
        return rules;
    }
    
    /**
     * 获取限流状态统计
     */
    private Map<String, Object> getRateLimitStatusStats() {
        Map<String, Object> status = new HashMap<>();
        // 这里需要根据实际的RateLimitService实现来获取状态统计
        status.put("normal", 0);
        status.put("warning", 0);
        status.put("critical", 0);
        return status;
    }
    
    /**
     * 获取限流历史数据
     */
    private Map<String, Object> getRateLimitHistory() {
        Map<String, Object> history = new HashMap<>();
        // 这里需要根据实际实现来获取历史数据
        history.put("period", "last_hour");
        history.put("data", new ArrayList<>());
        return history;
    }
    
    /**
     * 获取系统负载平均值
     */
    private double getSystemLoadAverage() {
        try {
            return java.lang.management.ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0;
        }
    }
    
    /**
     * 获取峰值RPS
     */
    private double getPeakRps() {
        // 实现峰值RPS计算
        return rateLimitMetrics.getRps() * 1.2; // 临时实现
    }
    
    /**
     * 获取平均RPS
     */
    private double getAverageRps() {
        // 实现平均RPS计算
        return rateLimitMetrics.getRps() * 0.8; // 临时实现
    }
    
    /**
     * 获取熔断器触发次数
     */
    private long getCircuitBreakerTripCount() {
        // 实现熔断器触发次数统计
        return 0L; // 临时实现
    }
}