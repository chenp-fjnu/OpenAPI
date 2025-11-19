package com.group.gateway.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求上下文服务
 * 负责存储和管理请求相关的上下文信息，包括认证信息、追踪ID等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class RequestContextService {
    
    // 上下文键常量
    private static final String CONTEXT_TRACE_ID = "traceId";
    private static final String CONTEXT_AUTH_INFO = "authInfo";
    private static final String CONTEXT_START_TIME = "startTime";
    private static final String CONTEXT_CLIENT_INFO = "clientInfo";
    private static final String CONTEXT_RATE_LIMIT_INFO = "rateLimitInfo";
    
    // 内存缓存，用于临时存储上下文信息
    private final Map<String, RequestContext> contextCache = new ConcurrentHashMap<>();
    
    /**
     * 生成链路追踪ID
     */
    public String generateTraceId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst("X-Trace-ID");
        
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            log.debug("生成新的追踪ID | traceId={}", traceId);
        } else {
            log.debug("复用现有追踪ID | traceId={}", traceId);
        }
        
        return traceId;
    }
    
    /**
     * 设置认证信息
     */
    public void setAuthInfo(ServerWebExchange exchange, AuthFilter.AuthResult authResult) {
        if (authResult != null) {
            String traceId = authResult.getTraceId();
            if (traceId != null) {
                RequestContext context = getOrCreateContext(traceId);
                context.setAuthInfo(authResult);
                
                log.debug("设置认证信息 | traceId={} | userId={} | roles={}", 
                    traceId, authResult.getUserId(), authResult.getRoles());
            }
        }
    }
    
    /**
     * 获取认证信息
     */
    public AuthFilter.AuthResult getAuthInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = contextCache.get(traceId);
            return context != null ? context.getAuthInfo() : null;
        }
        return null;
    }
    
    /**
     * 设置客户端信息
     */
    public void setClientInfo(ServerWebExchange exchange, ClientInfo clientInfo) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = getOrCreateContext(traceId);
            context.setClientInfo(clientInfo);
            
            log.debug("设置客户端信息 | traceId={} | clientIp={} | userAgent={}", 
                traceId, clientInfo.getClientIp(), clientInfo.getUserAgent());
        }
    }
    
    /**
     * 获取客户端信息
     */
    public ClientInfo getClientInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = contextCache.get(traceId);
            return context != null ? context.getClientInfo() : null;
        }
        return null;
    }
    
    /**
     * 设置限流信息
     */
    public void setRateLimitInfo(ServerWebExchange exchange, RateLimitInfo rateLimitInfo) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = getOrCreateContext(traceId);
            context.setRateLimitInfo(rateLimitInfo);
            
            log.debug("设置限流信息 | traceId={} | remaining={} | resetTime={}", 
                traceId, rateLimitInfo.getRemaining(), rateLimitInfo.getResetTime());
        }
    }
    
    /**
     * 获取限流信息
     */
    public RateLimitInfo getRateLimitInfo(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = contextCache.get(traceId);
            return context != null ? context.getRateLimitInfo() : null;
        }
        return null;
    }
    
    /**
     * 标记请求开始
     */
    public void markRequestStart(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = getOrCreateContext(traceId);
            context.setStartTime(Instant.now());
            
            log.debug("标记请求开始 | traceId={} | startTime={}", 
                traceId, context.getStartTime());
        }
    }
    
    /**
     * 获取请求开始时间
     */
    public Instant getStartTime(ServerWebExchange exchange) {
        String traceId = getTraceId(exchange);
        if (traceId != null) {
            RequestContext context = contextCache.get(traceId);
            return context != null ? context.getStartTime() : null;
        }
        return null;
    }
    
    /**
     * 创建或获取上下文
     */
    private RequestContext getOrCreateContext(String traceId) {
        return contextCache.computeIfAbsent(traceId, k -> {
            log.debug("创建新的请求上下文 | traceId={}", traceId);
            return new RequestContext(traceId);
        });
    }
    
    /**
     * 获取追踪ID
     */
    private String getTraceId(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("X-Trace-ID");
    }
    
    /**
     * 清理上下文信息
     */
    public void cleanupContext(String traceId) {
        if (traceId != null) {
            RequestContext context = contextCache.remove(traceId);
            if (context != null) {
                log.debug("清理请求上下文 | traceId={} | duration={}ms", 
                    traceId, context.getDuration());
            }
        }
    }
    
    /**
     * 获取上下文信息
     */
    public RequestContext getContext(String traceId) {
        return contextCache.get(traceId);
    }
    
    /**
     * 获取所有上下文信息（用于监控）
     */
    public Map<String, RequestContext> getAllContexts() {
        return new ConcurrentHashMap<>(contextCache);
    }
    
    /**
     * 清理过期上下文
     */
    public void cleanupExpiredContexts() {
        Instant now = Instant.now();
        contextCache.entrySet().removeIf(entry -> {
            RequestContext context = entry.getValue();
            // 清理超过1小时的上下文
            if (context.getStartTime() != null) {
                long minutesElapsed = java.time.Duration.between(context.getStartTime(), now).toMinutes();
                return minutesElapsed > 60;
            }
            return false;
        });
        
        log.debug("清理过期上下文，当前缓存数量: {}", contextCache.size());
    }
}