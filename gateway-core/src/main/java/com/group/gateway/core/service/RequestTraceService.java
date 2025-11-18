package com.group.gateway.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求追踪服务
 * 负责记录和分析API请求的完整生命周期，包括请求开始、结束、耗时、状态等信息
 * 支持链路追踪和监控指标收集
 * 
 * @author system
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestTraceService {

    /**
     * 请求追踪信息数据模型
     */
    public static class RequestTraceInfo {
        private String traceId;
        private Instant requestTime;
        private Instant endTime;
        private String method;
        private String path;
        private String query;
        private String clientIp;
        private String userAgent;
        private String userId;
        private String tenantId;
        private String clientId;
        private int statusCode;
        private long duration;
        private boolean success;
        private String errorMessage;
        private String errorClass;

        // Getters and Setters
        public String getTraceId() { return traceId; }
        public void setTraceId(String traceId) { this.traceId = traceId; }

        public Instant getRequestTime() { return requestTime; }
        public void setRequestTime(Instant requestTime) { this.requestTime = requestTime; }

        public Instant getEndTime() { return endTime; }
        public void setEndTime(Instant endTime) { this.endTime = endTime; }

        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }

        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public int getStatusCode() { return statusCode; }
        public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getErrorClass() { return errorClass; }
        public void setErrorClass(String errorClass) { this.errorClass = errorClass; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RequestTraceInfo info = new RequestTraceInfo();

            public Builder traceId(String traceId) {
                info.traceId = traceId;
                return this;
            }

            public Builder requestTime(Instant requestTime) {
                info.requestTime = requestTime;
                return this;
            }

            public Builder method(String method) {
                info.method = method;
                return this;
            }

            public Builder path(String path) {
                info.path = path;
                return this;
            }

            public Builder query(String query) {
                info.query = query;
                return this;
            }

            public Builder clientIp(String clientIp) {
                info.clientIp = clientIp;
                return this;
            }

            public Builder userAgent(String userAgent) {
                info.userAgent = userAgent;
                return this;
            }

            public Builder userId(String userId) {
                info.userId = userId;
                return this;
            }

            public Builder tenantId(String tenantId) {
                info.tenantId = tenantId;
                return this;
            }

            public Builder clientId(String clientId) {
                info.clientId = clientId;
                return this;
            }

            public RequestTraceInfo build() {
                return info;
            }
        }
    }
    
    // 内存缓存用于存储请求开始信息（生产环境应使用Redis或数据库）
    private final Map<String, RequestTraceInfo> traceInfoCache = new ConcurrentHashMap<>();
    
    /**
     * 记录请求开始
     */
    public Mono<Void> recordRequestStart(ServerWebExchange exchange) {
        return Mono.fromRunnable(() -> {
            try {
                ServerHttpRequest request = exchange.getRequest();
                String traceId = getTraceId(request);
                
                if (traceId != null) {
                    RequestTraceInfo traceInfo = RequestTraceInfo.builder()
                        .traceId(traceId)
                        .requestTime(Instant.now())
                        .method(request.getMethod() != null ? request.getMethod().name() : "UNKNOWN")
                        .path(request.getURI().getPath())
                        .query(request.getURI().getQuery())
                        .clientIp(getClientIp(request))
                        .userAgent(getUserAgent(request))
                        .userId(getHeader(request, "X-User-ID"))
                        .tenantId(getHeader(request, "X-Tenant-ID"))
                        .clientId(getHeader(request, "X-Client-ID"))
                        .build();
                    
                    traceInfoCache.put(traceId, traceInfo);
                    
                    log.debug("记录请求开始 | traceId={} | method={} | path={}", 
                        traceId, traceInfo.getMethod(), traceInfo.getPath());
                }
            } catch (Exception e) {
                log.error("记录请求开始时发生异常", e);
            }
        });
    }
    
    /**
     * 记录请求成功
     */
    public Mono<Void> recordRequestSuccess(ServerWebExchange exchange, long duration) {
        return Mono.fromRunnable(() -> {
            try {
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
                String traceId = getTraceId(request);
                
                if (traceId != null) {
                    RequestTraceInfo traceInfo = traceInfoCache.get(traceId);
                    if (traceInfo != null) {
                        traceInfo.setStatusCode(response.getStatusCode() != null ? 
                            response.getStatusCode().value() : 0);
                        traceInfo.setDuration(duration);
                        traceInfo.setSuccess(true);
                        traceInfo.setEndTime(Instant.now());
                        
                        // 记录到日志
                        log.info("请求追踪完成 | traceId={} | statusCode={} | duration={}ms | success={}",
                            traceId, traceInfo.getStatusCode(), duration, true);
                        
                        // 发送监控指标（异步）
                        sendMetrics(traceInfo, "success");
                        
                        // 清理缓存（可选择保留一段时间用于调试）
                        traceInfoCache.remove(traceId);
                    }
                }
            } catch (Exception e) {
                log.error("记录请求成功时发生异常", e);
            }
        });
    }
    
    /**
     * 记录请求错误
     */
    public Mono<Void> recordRequestError(ServerWebExchange exchange, long duration, Throwable error) {
        return Mono.fromRunnable(() -> {
            try {
                ServerHttpRequest request = exchange.getRequest();
                String traceId = getTraceId(request);
                
                if (traceId != null) {
                    RequestTraceInfo traceInfo = traceInfoCache.get(traceId);
                    if (traceInfo != null) {
                        traceInfo.setStatusCode(getErrorStatusCode(error));
                        traceInfo.setDuration(duration);
                        traceInfo.setSuccess(false);
                        traceInfo.setEndTime(Instant.now());
                        traceInfo.setErrorMessage(error.getMessage());
                        traceInfo.setErrorClass(error.getClass().getSimpleName());
                        
                        // 记录到日志
                        log.error("请求追踪错误 | traceId={} | statusCode={} | duration={}ms | error={} | success={}",
                            traceId, traceInfo.getStatusCode(), duration, error.getMessage(), false);
                        
                        // 发送监控指标（异步）
                        sendMetrics(traceInfo, "error");
                        
                        // 清理缓存
                        traceInfoCache.remove(traceId);
                    }
                }
            } catch (Exception e) {
                log.error("记录请求错误时发生异常", e);
            }
        });
    }
    
    /**
     * 根据错误信息获取HTTP状态码
     */
    private int getErrorStatusCode(Throwable error) {
        // 这里可以添加更复杂的错误状态码映射逻辑
        if (error instanceof org.springframework.web.server.ResponseStatusException) {
            return ((org.springframework.web.server.ResponseStatusException) error).getStatusCode().value();
        }
        return 500; // 默认服务器内部错误
    }
    
    /**
     * 发送监控指标
     */
    private void sendMetrics(RequestTraceInfo traceInfo, String result) {
        // 这里可以集成Prometheus、Graphite等监控系统
        // 例如：prometheusRegistry.counter("gateway_requests_total", "result", result).increment();
        
        log.debug("发送监控指标 | traceId={} | result={} | duration={}ms", 
            traceInfo.getTraceId(), result, traceInfo.getDuration());
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = getHeader(request, "X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = getHeader(request, "X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
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
        return getHeader(request, "User-Agent");
    }
    
    /**
     * 获取请求头中的值
     */
    private String getHeader(ServerHttpRequest request, String name) {
        return request.getHeaders().getFirst(name);
    }
    
    /**
     * 获取链路追踪ID
     */
    private String getTraceId(ServerHttpRequest request) {
        return getHeader(request, "X-Trace-ID");
    }
    
    /**
     * 获取请求追踪信息（用于调试）
     */
    public RequestTraceInfo getTraceInfo(String traceId) {
        return traceInfoCache.get(traceId);
    }
    
    /**
     * 获取所有追踪信息（用于监控界面）
     */
    public Map<String, RequestTraceInfo> getAllTraceInfo() {
        return new ConcurrentHashMap<>(traceInfoCache);
    }
    
    /**
     * 清理过期追踪信息（建议定时任务调用）
     */
    public void cleanExpiredTraceInfo() {
        Instant now = Instant.now();
        traceInfoCache.entrySet().removeIf(entry -> {
            RequestTraceInfo info = entry.getValue();
            // 清理超过1小时的追踪信息
            return Duration.between(info.getRequestTime(), now).toHours() > 1;
        });
        
        log.debug("清理过期追踪信息完成，当前缓存数量: {}", traceInfoCache.size());
    }
}