package com.group.gateway.core.controller;

import com.group.gateway.common.response.Result;
import com.group.gateway.common.response.ResultCode;
import com.group.gateway.core.service.RateLimitMetrics;
import com.group.gateway.core.service.RequestTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 提供系统健康状态、指标统计和状态信息查询
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {
    
    private final RequestTraceService requestTraceService;
    private final RateLimitMetrics rateLimitMetrics;
    
    /**
     * 基础健康检查
     */
    @GetMapping("/check")
    public Mono<ResponseEntity<Result<Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "gateway-core");
        health.put("version", "1.0.0");
        
        return Mono.just(ResponseEntity.ok(Result.success(health)));
    }
    
    /**
     * 详细健康检查
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> detailedHealthCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Object> status = new HashMap<>();
            
            // 基础状态
            status.put("status", "UP");
            status.put("timestamp", LocalDateTime.now());
            status.put("service", "gateway-core");
            status.put("version", "1.0.0");
            
            // 限流状态
            Map<String, Object> rateLimitStatus = new HashMap<>();
            rateLimitStatus.put("enabled", true);
            rateLimitStatus.put("totalRequests", rateLimitMetrics.getTotalRequests());
            rateLimitStatus.put("acceptedRequests", rateLimitMetrics.getAcceptedRequests());
            rateLimitStatus.put("rejectedRequests", rateLimitMetrics.getRejectedRequests());
            rateLimitStatus.put("acceptanceRate", rateLimitMetrics.getAcceptanceRate());
            rateLimitStatus.put("rejectionRate", rateLimitMetrics.getRejectionRate());
            status.put("rateLimit", rateLimitStatus);
            
            // 请求追踪状态
            Map<String, Object> traceStatus = new HashMap<>();
            traceStatus.put("enabled", true);
            traceStatus.put("totalTraces", requestTraceService.getTotalTraces());
            traceStatus.put("activeTraces", requestTraceService.getActiveTraces());
            traceStatus.put("averageResponseTime", requestTraceService.getAverageResponseTime());
            status.put("tracing", traceStatus);
            
            return ResponseEntity.ok(Result.success(status));
        });
    }
    
    /**
     * 就绪性检查
     */
    @GetMapping("/ready")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> readinessCheck() {
        return Mono.fromCallable(() -> {
            Map<String, Object> readiness = new HashMap<>();
            
            // 检查各项服务的就绪状态
            boolean redisReady = checkRedisConnection();
            boolean nacosReady = checkNacosConnection();
            boolean metricsReady = rateLimitMetrics.isHealthHealthy();
            
            readiness.put("timestamp", LocalDateTime.now());
            readiness.put("redis", redisReady ? "READY" : "NOT_READY");
            readiness.put("nacos", nacosReady ? "READY" : "NOT_READY");
            readiness.put("metrics", metricsReady ? "READY" : "NOT_READY");
            readiness.put("overall", (redisReady && nacosReady && metricsReady) ? "READY" : "NOT_READY");
            
            HttpStatus status = (redisReady && nacosReady && metricsReady) ? 
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            
            return ResponseEntity.status(status).body(Result.success(readiness));
        });
    }
    
    /**
     * 存活检查
     */
    @GetMapping("/live")
    public Mono<ResponseEntity<Result<Map<String, Object>>>> livenessCheck() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", LocalDateTime.now());
        liveness.put("uptime", System.currentTimeMillis());
        
        return Mono.just(ResponseEntity.ok(Result.success(liveness)));
    }
    
    /**
     * 检查Redis连接状态
     */
    private boolean checkRedisConnection() {
        try {
            // 简单的Redis连接检查
            return true; // 实际实现中需要连接Redis并执行ping命令
        } catch (Exception e) {
            log.warn("Redis连接检查失败", e);
            return false;
        }
    }
    
    /**
     * 检查Nacos连接状态
     */
    private boolean checkNacosConnection() {
        try {
            // 简单的Nacos连接检查
            return true; // 实际实现中需要连接Nacos并查询服务
        } catch (Exception e) {
            log.warn("Nacos连接检查失败", e);
            return false;
        }
    }
}