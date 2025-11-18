package com.group.gateway.core.service;

import com.group.gateway.core.filter.AuthFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 请求上下文服务
 * 负责存储和管理请求相关上下文信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class RequestContextService {
    
    /**
     * 上下文存储
     */
    private final ConcurrentMap<String, RequestContext> contexts = new ConcurrentHashMap<>();
    
    /**
     * 上下文键常量
     */
    public static final String CONTEXT_KEY = "request_context";
    public static final String TRACE_ID_KEY = "trace_id";
    public static final String AUTH_INFO_KEY = "auth_info";
    public static final String CLIENT_INFO_KEY = "client_info";
    public static final String RATE_LIMIT_INFO_KEY = "rate_limit_info";
    public static final String START_TIME_KEY = "start_time";
    
    /**
     * 过期时间（毫秒）
     */
    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1小时
    
    /**
     * 生成链路追踪ID
     */
    public String generateTraceId() {
        return "trace_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }
    
    /**
     * 设置认证信息
     */
    public void setAuthInfo(String traceId, AuthFilter.AuthResult authResult) {
        if (traceId != null && authResult != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.setAuthInfo(authResult);
            }
        }
    }
    
    /**
     * 获取认证信息
     */
    public AuthFilter.AuthResult getAuthInfo(String traceId) {
        RequestContext context = getContext(traceId);
        return context != null ? context.getAuthInfo() : null;
    }
    
    /**
     * 设置客户端信息
     */
    public void setClientInfo(String traceId, ClientInfo clientInfo) {
        if (traceId != null && clientInfo != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.setClientInfo(clientInfo);
            }
        }
    }
    
    /**
     * 获取客户端信息
     */
    public ClientInfo getClientInfo(String traceId) {
        RequestContext context = getContext(traceId);
        return context != null ? context.getClientInfo() : null;
    }
    
    /**
     * 设置限流信息
     */
    public void setRateLimitInfo(String traceId, RateLimitInfo rateLimitInfo) {
        if (traceId != null && rateLimitInfo != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.setRateLimitInfo(rateLimitInfo);
            }
        }
    }
    
    /**
     * 获取限流信息
     */
    public RateLimitInfo getRateLimitInfo(String traceId) {
        RequestContext context = getContext(traceId);
        return context != null ? context.getRateLimitInfo() : null;
    }
    
    /**
     * 标记请求开始
     */
    public void markRequestStart(String traceId) {
        if (traceId != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.setStartTime(java.time.Instant.now());
                context.setStatus(RequestContext.ProcessingStatus.PROCESSING);
            }
        }
    }
    
    /**
     * 标记请求完成
     */
    public void markRequestComplete(String traceId) {
        if (traceId != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.markComplete();
            }
        }
    }
    
    /**
     * 标记请求失败
     */
    public void markRequestFailed(String traceId, String errorMessage) {
        if (traceId != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.markFailed(errorMessage);
            }
        }
    }
    
    /**
     * 设置业务信息
     */
    public void setBusinessInfo(String traceId, String businessModule, String targetService, String targetUrl) {
        if (traceId != null) {
            RequestContext context = getContext(traceId);
            if (context != null) {
                context.setBusinessModule(businessModule);
                context.setTargetService(targetService);
                context.setTargetUrl(targetUrl);
            }
        }
    }
    
    /**
     * 获取上下文
     */
    public RequestContext getContext(String traceId) {
        return traceId != null ? contexts.get(traceId) : null;
    }
    
    /**
     * 创建新的上下文
     */
    public RequestContext createContext(String traceId) {
        if (traceId == null) {
            return null;
        }
        
        RequestContext context = new RequestContext(traceId);
        RequestContext existing = contexts.putIfAbsent(traceId, context);
        return existing != null ? existing : context;
    }
    
    /**
     * 清理过期上下文
     */
    public void cleanupExpiredContexts() {
        long currentTime = System.currentTimeMillis();
        contexts.entrySet().removeIf(entry -> {
            RequestContext context = entry.getValue();
            return context.getStartTime() != null && 
                   (currentTime - context.getStartTime().toEpochMilli()) > EXPIRATION_TIME;
        });
        
        log.debug("清理过期上下文完成，当前活跃上下文数量: {}", contexts.size());
    }
    
    /**
     * 清理上下文
     */
    public void cleanupContext(String traceId) {
        if (traceId != null) {
            contexts.remove(traceId);
            log.debug("清理上下文: {}", traceId);
        }
    }
    
    /**
     * 获取所有上下文（用于监控）
     */
    public ConcurrentHashMap<String, RequestContext> getAllContexts() {
        return new ConcurrentHashMap<>(contexts);
    }
    
    /**
     * 获取上下文统计信息
     */
    public ContextStatistics getContextStatistics() {
        int total = contexts.size();
        int pending = 0;
        int processing = 0;
        int completed = 0;
        int failed = 0;
        long totalDuration = 0;
        long completedCount = 0;
        
        for (RequestContext context : contexts.values()) {
            switch (context.getStatus()) {
                case PENDING:
                    pending++;
                    break;
                case PROCESSING:
                    processing++;
                    break;
                case COMPLETED:
                    completed++;
                    if (context.getDuration() != null) {
                        totalDuration += context.getDuration();
                        completedCount++;
                    }
                    break;
                case FAILED:
                    failed++;
                    break;
            }
        }
        
        double avgDuration = completedCount > 0 ? (double) totalDuration / completedCount : 0;
        
        return new ContextStatistics(total, pending, processing, completed, failed, avgDuration);
    }
    
    /**
     * 上下文统计信息
     */
    public static class ContextStatistics {
        private final int total;
        private final int pending;
        private final int processing;
        private final int completed;
        private final int failed;
        private final double avgDuration;
        
        public ContextStatistics(int total, int pending, int processing, int completed, int failed, double avgDuration) {
            this.total = total;
            this.pending = pending;
            this.processing = processing;
            this.completed = completed;
            this.failed = failed;
            this.avgDuration = avgDuration;
        }
        
        public int getTotal() { return total; }
        public int getPending() { return pending; }
        public int getProcessing() { return processing; }
        public int getCompleted() { return completed; }
        public int getFailed() { return failed; }
        public double getAvgDuration() { return avgDuration; }
        
        @Override
        public String toString() {
            return String.format(
                "ContextStatistics{total=%d, pending=%d, processing=%d, completed=%d, failed=%d, avgDuration=%.2fms}",
                total, pending, processing, completed, failed, avgDuration
            );
        }
    }
}