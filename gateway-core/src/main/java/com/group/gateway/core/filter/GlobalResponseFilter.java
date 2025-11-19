package com.group.gateway.core.filter;

import com.group.gateway.core.service.RequestTraceService;
import com.group.gateway.core.service.RequestContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * 全局响应过滤器
 * 负责响应后处理、监控指标收集、响应头添加、错误处理等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class GlobalResponseFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private RequestTraceService requestTraceService;
    
    @Autowired
    private RequestContextService requestContextService;
    
    private static final String TRACE_ID_HEADER = "X-Trace-ID";
    private static final String RESPONSE_TIME_HEADER = "X-Response-Time";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String RATE_LIMIT_HEADER = "X-Rate-Limit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-Rate-Limit-Reset";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).doOnSuccess(aVoid -> {
            try {
                processResponse(exchange, startTime);
            } catch (Exception e) {
                log.error("响应后处理异常", e);
            }
        }).doOnError(throwable -> {
            try {
                processErrorResponse(exchange, startTime, throwable);
            } catch (Exception e) {
                log.error("错误响应处理异常", e);
            }
        });
    }
    
    /**
     * 处理正常响应
     */
    private void processResponse(ServerWebExchange exchange, long startTime) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 添加响应头信息
        addResponseHeaders(exchange, responseTime);
        
        // 记录链路追踪
        String traceId = requestContextService.getTraceId(exchange);
        if (traceId != null) {
            requestTraceService.recordSuccess(traceId, responseTime, response.getStatusCode() != null ? response.getStatusCode().value() : 200);
        }
        
        // 记录访问日志
        recordAccessLog(request, response, responseTime, true);
        
        // 检查慢请求
        if (responseTime > 5000) { // 超过5秒的请求
            log.warn("慢请求检测: traceId={}, path={}, responseTime={}ms", 
                    traceId, request.getURI().getPath(), responseTime);
        }
        
        log.debug("请求处理完成: traceId={}, method={}, path={}, status={}, responseTime={}ms", 
                traceId, request.getMethodValue(), request.getURI().getPath(), 
                response.getStatusCode() != null ? response.getStatusCode().value() : "unknown", responseTime);
    }
    
    /**
     * 处理错误响应
     */
    private void processErrorResponse(ServerWebExchange exchange, long startTime, Throwable throwable) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 确保响应状态正确设置
        if (response.getStatusCode() == null) {
            response.setStatusCode(getErrorStatusCode(throwable));
        }
        
        // 添加响应头信息
        addResponseHeaders(exchange, responseTime);
        
        // 记录链路追踪失败
        String traceId = requestContextService.getTraceId(exchange);
        if (traceId != null) {
            requestTraceService.recordFailure(traceId, responseTime, 
                    response.getStatusCode() != null ? response.getStatusCode().value() : 500);
        }
        
        // 记录访问日志
        recordAccessLog(request, response, responseTime, false);
        
        log.error("请求处理失败: traceId={}, method={}, path={}, error={}, responseTime={}ms", 
                traceId, request.getMethodValue(), request.getURI().getPath(), 
                throwable.getMessage(), responseTime);
    }
    
    /**
     * 添加响应头
     */
    private void addResponseHeaders(ServerWebExchange exchange, long responseTime) {
        ServerHttpResponse response = exchange.getResponse();
        ServerHttpRequest request = exchange.getRequest();
        
        // 添加链路追踪ID
        String traceId = requestContextService.getTraceId(exchange);
        if (traceId != null) {
            response.getHeaders().set(TRACE_ID_HEADER, traceId);
        }
        
        // 添加请求ID
        String requestId = exchange.getAttribute(REQUEST_ID_HEADER);
        if (requestId != null) {
            response.getHeaders().set(REQUEST_ID_HEADER, requestId);
        }
        
        // 添加响应时间
        response.getHeaders().set(RESPONSE_TIME_HEADER, responseTime + "ms");
        
        // 添加限流相关信息
        Long remainingRateLimit = exchange.getAttribute("remainingRateLimit");
        if (remainingRateLimit != null) {
            response.getHeaders().set(RATE_LIMIT_HEADER, remainingRateLimit.toString());
        }
        
        Long rateLimitReset = exchange.getAttribute("rateLimitReset");
        if (rateLimitReset != null) {
            response.getHeaders().set(RATE_LIMIT_RESET_HEADER, String.valueOf(rateLimitReset));
        }
        
        // 添加安全头
        addSecurityHeaders(response);
        
        // 添加缓存控制头
        addCacheControlHeaders(exchange, response);
    }
    
    /**
     * 添加安全头
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        response.getHeaders().set("X-Content-Type-Options", "nosniff");
        response.getHeaders().set("X-Frame-Options", "DENY");
        response.getHeaders().set("X-XSS-Protection", "1; mode=block");
        response.getHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.getHeaders().set("Content-Security-Policy", "default-src 'self'");
    }
    
    /**
     * 添加缓存控制头
     */
    private void addCacheControlHeaders(ServerWebExchange exchange, ServerHttpResponse response) {
        String path = exchange.getRequest().getURI().getPath();
        
        // 管理API和敏感接口不缓存
        if (path.startsWith("/admin") || path.contains("auth") || path.contains("logout")) {
            response.getHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            response.getHeaders().set("Pragma", "no-cache");
            response.getHeaders().set("Expires", "0");
        } else if (path.contains("health") || path.contains("metrics")) {
            // 监控接口短时间缓存
            response.getHeaders().set("Cache-Control", "public, max-age=30");
        } else {
            // 默认不缓存
            response.getHeaders().set("Cache-Control", "no-cache");
        }
    }
    
    /**
     * 记录访问日志
     */
    private void recordAccessLog(ServerHttpRequest request, ServerHttpResponse response, 
                                long responseTime, boolean success) {
        try {
            String clientIp = getClientIp(request);
            String userAgent = request.getHeaders().getFirst("User-Agent");
            String method = request.getMethodValue();
            String path = request.getURI().getPath();
            int status = response.getStatusCode() != null ? response.getStatusCode().value() : 200;
            String traceId = requestContextService.getTraceId(request);
            
            String logMessage = String.format(
                "访问日志 - IP: %s, 方法: %s, 路径: %s, 状态: %d, 响应时间: %dms, 链路ID: %s, UA: %s, 成功: %s",
                clientIp, method, path, status, responseTime, traceId, 
                userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 50)) : "Unknown", 
                success
            );
            
            if (success) {
                log.info(logMessage);
            } else {
                log.error(logMessage);
            }
        } catch (Exception e) {
            log.error("记录访问日志异常", e);
        }
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null ? 
                request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
    }
    
    /**
     * 获取错误状态码
     */
    private org.springframework.http.HttpStatus getErrorStatusCode(Throwable throwable) {
        if (throwable instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            return org.springframework.http.HttpStatus.NOT_FOUND;
        } else if (throwable instanceof org.springframework.cloud.gateway.support.TimeoutException) {
            return org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
        } else if (throwable instanceof org.springframework.web.server.ResponseStatusException) {
            return ((org.springframework.web.server.ResponseStatusException) throwable).getStatus();
        } else {
            return org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    @Override
    public int getOrder() {
        return 100; // 在其他过滤器之后执行
    }
}