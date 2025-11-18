package com.group.gateway.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ServerWebExchange;
import com.group.gateway.core.filter.AuthFilter.AuthResult;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求上下文服务
 * 用于存储和管理请求相关的上下文信息，包括认证信息、追踪ID、请求时间等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestContextService {
    
    /**
     * 上下文缓存
     */
    private final ConcurrentHashMap<String, RequestContext> contextCache = new ConcurrentHashMap<>();
    
    /**
     * 上下文键常量
     */
    public static final String CONTEXT_TRACE_ID = "traceId";
    public static final String CONTEXT_AUTH_INFO = "authInfo";
    public static final String CONTEXT_CLIENT_INFO = "clientInfo";
    public static final String CONTEXT_RATE_LIMIT_INFO = "rateLimitInfo";
    public static final String CONTEXT_REQUEST_TIME = "requestTime";
    
    /**
     * 生成链路追踪ID
     */
    public String generateTraceId() {
        return "trace_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
    
    /**
     * 设置认证信息
     */
    public void setAuthInfo(ServerWebExchange exchange, AuthResult authInfo) {
        String traceId = getOrCreateTraceId(exchange);
        RequestContext context = getOrCreateContext(traceId);
        context.setAuthInfo(authInfo);
        contextCache.put(traceId, context);
    }
    
    /**
     * 获取认证信息
     */
    public AuthResult getAuthInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return null;
        }
        
        RequestContext context = contextCache.get(traceId);
        return context != null ? context.getAuthInfo() : null;
    }
    
    /**
     * 设置客户端信息
     */
    public void setClientInfo(ServerWebExchange exchange, ClientInfo clientInfo) {
        String traceId = getOrCreateTraceId(exchange);
        RequestContext context = getOrCreateContext(traceId);
        context.setClientInfo(clientInfo);
        contextCache.put(traceId, context);
    }
    
    /**
     * 获取客户端信息
     */
    public ClientInfo getClientInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return null;
        }
        
        RequestContext context = contextCache.get(traceId);
        return context != null ? context.getClientInfo() : null;
    }
    
    /**
     * 设置限流信息
     */
    public void setRateLimitInfo(ServerWebExchange exchange, RateLimitInfo rateLimitInfo) {
        String traceId = getOrCreateTraceId(exchange);
        RequestContext context = getOrCreateContext(traceId);
        context.setRateLimitInfo(rateLimitInfo);
        contextCache.put(traceId, context);
    }
    
    /**
     * 获取限流信息
     */
    public RateLimitInfo getRateLimitInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return null;
        }
        
        RequestContext context = contextCache.get(traceId);
        return context != null ? context.getRateLimitInfo() : null;
    }
    
    /**
     * 标记请求开始
     */
    public void markRequestStart(ServerWebExchange exchange) {
        String traceId = getOrCreateTraceId(exchange);
        RequestContext context = getOrCreateContext(traceId);
        context.setStartTime(Instant.now());
        context.setStatus(RequestContext.ProcessingStatus.PROCESSING);
        contextCache.put(traceId, context);
        
        log.debug("请求开始 | traceId={} | path={}", traceId, exchange.getRequest().getURI().getPath());
    }
    
    /**
     * 标记请求完成
     */
    public void markRequestComplete(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return;
        }
        
        RequestContext context = contextCache.get(traceId);
        if (context != null) {
            context.markComplete();
            log.debug("请求完成 | traceId={} | duration={}ms", traceId, context.getDuration());
        }
    }
    
    /**
     * 标记请求失败
     */
    public void markRequestFailed(ServerWebExchange exchange, String errorMessage) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return;
        }
        
        RequestContext context = contextCache.get(traceId);
        if (context != null) {
            context.markFailed(errorMessage);
            log.warn("请求失败 | traceId={} | error={}", traceId, errorMessage);
        }
    }
    
    /**
     * 获取请求上下文
     */
    public RequestContext getRequestContext(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            return null;
        }
        
        return contextCache.get(traceId);
    }
    
    /**
     * 获取或创建追踪ID
     */
    private String getOrCreateTraceId(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId == null) {
            traceId = generateTraceId();
            ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
            builder.header("X-Trace-ID", traceId);
            ServerWebExchange newExchange = exchange.mutate()
                .request(builder.build())
                .build();
            
            // 注意：这里不能直接修改exchange，需要在调用方处理
            exchange.getAttributes().put("X-Trace-ID", traceId);
        }
        return traceId;
    }
    
    /**
     * 获取追踪ID
     */
    private String getTraceId(ServerWebExchange exchange) {
        // 首先从请求头获取
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-ID");
        if (traceId != null) {
            return traceId;
        }
        
        // 如果请求头中没有，从属性中获取
        traceId = (String) exchange.getAttributes().get("X-Trace-ID");
        return traceId;
    }
    
    /**
     * 获取或创建请求上下文
     */
    private RequestContext getOrCreateContext(String traceId) {
        RequestContext context = contextCache.get(traceId);
        if (context == null) {
            context = new RequestContext(traceId);
        }
        return context;
    }
    
    /**
     * 清理过期上下文
     */
    public void cleanupExpiredContexts() {
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - (60 * 60 * 1000); // 1小时前
        
        contextCache.entrySet().removeIf(entry -> {
            RequestContext context = entry.getValue();
            return context.getStartTime() != null && 
                   context.getStartTime().toEpochMilli() < expireTime;
        });
        
        log.debug("清理过期上下文完成 | 当前缓存大小={}", contextCache.size());
    }
    
    /**
     * 获取当前上下文统计信息
     */
    public String getContextStatistics() {
        int totalContexts = contextCache.size();
        long completedContexts = contextCache.values().stream()
            .mapToLong(context -> context.getStatus() == RequestContext.ProcessingStatus.COMPLETED ? 1 : 0)
            .sum();
        long failedContexts = contextCache.values().stream()
            .mapToLong(context -> context.getStatus() == RequestContext.ProcessingStatus.FAILED ? 1 : 0)
            .sum();
            
        return String.format("总上下文: %d, 完成: %d, 失败: %d", 
            totalContexts, completedContexts, failedContexts);
    }
}