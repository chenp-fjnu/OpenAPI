package com.group.gateway.core.filter;

import com.group.gateway.common.response.Result;
import com.group.gateway.common.response.ResultCode;
import com.group.gateway.core.service.RequestTraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 全局请求过滤器
 * 负责请求的预处理、链路追踪、请求日志记录等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRequestFilter implements GlobalFilter, Ordered {
    
    private static final String TRACE_ID_KEY = "X-Trace-ID";
    private static final String USER_ID_KEY = "X-User-ID";
    private static final String TENANT_ID_KEY = "X-Tenant-ID";
    private static final String CLIENT_ID_KEY = "X-Client-ID";
    private static final String REQUEST_START_TIME_KEY = "X-Request-Start-Time";
    
    private static final List<String> SENSITIVE_HEADERS = List.of(
        "Authorization", "X-Auth-Token", "Cookie", "Set-Cookie"
    );
    
    private final RequestTraceService requestTraceService;
    
    @Override
    public int getOrder() {
        // 设置为优先级最高的过滤器
        return Ordered.HIGHEST_PRECEDENCE;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // 1. 生成或获取链路追踪ID
        String traceId = generateTraceId(exchange);
        
        // 2. 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        // 3. 记录请求日志
        logRequest(request, traceId);
        
        // 4. 添加链路追踪信息到请求头
        ServerHttpRequest mutatedRequest = request.mutate()
            .header(TRACE_ID_KEY, traceId)
            .header(REQUEST_START_TIME_KEY, String.valueOf(startTime))
            .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .response(response)
            .build();
        
        // 5. 记录链路追踪
        return requestTraceService.recordRequestStart(mutatedExchange)
            .then(chain.filter(mutatedExchange))
            .doOnSuccess(aVoid -> {
                // 请求成功时的处理
                recordRequestSuccess(mutatedExchange, startTime);
            })
            .doOnError(error -> {
                // 请求失败时的处理
                recordRequestError(mutatedExchange, startTime, error);
            });
    }
    
    /**
     * 生成链路追踪ID
     */
    private String generateTraceId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 先检查请求头中是否已有追踪ID
        String traceId = request.getHeaders().getFirst(TRACE_ID_KEY);
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        
        // 检查是否来自内部服务调用
        String internalTraceId = request.getHeaders().getFirst("X-Internal-Trace-ID");
        if (StringUtils.hasText(internalTraceId)) {
            return internalTraceId;
        }
        
        // 生成新的追踪ID
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 记录请求日志
     */
    private void logRequest(ServerHttpRequest request, String traceId) {
        if (!log.isInfoEnabled()) {
            return;
        }
        
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);
        String userId = request.getHeaders().getFirst(USER_ID_KEY);
        String tenantId = request.getHeaders().getFirst(TENANT_ID_KEY);
        String clientId = request.getHeaders().getFirst(CLIENT_ID_KEY);
        
        // 过滤敏感信息
        String filteredHeaders = filterSensitiveHeaders(request.getHeaders());
        
        log.info("收到请求 | traceId={} | method={} | path={} | query={} | clientIp={} | userAgent={} | userId={} | tenantId={} | clientId={} | headers={}",
            traceId, method, path, query, clientIp, userAgent, userId, tenantId, clientId, filteredHeaders);
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
            ? request.getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
    
    /**
     * 获取用户代理
     */
    private String getUserAgent(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst("User-Agent");
        return userAgent != null ? userAgent : "unknown";
    }
    
    /**
     * 过滤敏感请求头信息
     */
    private String filterSensitiveHeaders(org.springframework.http.HttpHeaders headers) {
        if (headers.isEmpty()) {
            return "{}";
        }
        
        StringBuilder filtered = new StringBuilder("{");
        headers.forEach((key, values) -> {
            if (!SENSITIVE_HEADERS.contains(key)) {
                filtered.append(key).append("=").append(values).append(", ");
            }
        });
        if (filtered.length() > 1) {
            filtered.setLength(filtered.length() - 2);
        }
        filtered.append("}");
        return filtered.toString();
    }
    
    /**
     * 记录请求成功
     */
    private void recordRequestSuccess(ServerWebExchange exchange, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        String traceId = getTraceId(request);
        int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        
        log.info("请求完成 | traceId={} | method={} | path={} | statusCode={} | duration={}ms",
            traceId, 
            request.getMethod() != null ? request.getMethod().name() : "UNKNOWN",
            request.getURI().getPath(),
            statusCode,
            duration);
        
        // 记录链路追踪成功
        requestTraceService.recordRequestSuccess(exchange, duration);
    }
    
    /**
     * 记录请求错误
     */
    private void recordRequestError(ServerWebExchange exchange, long startTime, Throwable error) {
        long duration = System.currentTimeMillis() - startTime;
        ServerHttpRequest request = exchange.getRequest();
        
        String traceId = getTraceId(request);
        String errorMessage = error.getMessage();
        
        log.error("请求异常 | traceId={} | method={} | path={} | duration={}ms | error={}",
            traceId,
            request.getMethod() != null ? request.getMethod().name() : "UNKNOWN",
            request.getURI().getPath(),
            duration,
            errorMessage,
            error);
        
        // 记录链路追踪错误
        requestTraceService.recordRequestError(exchange, duration, error);
    }
    
    /**
     * 从请求头中获取追踪ID
     */
    private String getTraceId(ServerHttpRequest request) {
        return request.getHeaders().getFirst(TRACE_ID_KEY);
    }
    
    /**
     * 处理OPTIONS预检请求
     */
    private Mono<Void> handleOptionsRequest(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range,Authorization");
        response.getHeaders().add("Access-Control-Expose-Headers", "Content-Length,Content-Range");
        return response.setComplete();
    }
}