package com.group.gateway.core.filter;

import com.group.gateway.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * 限流过滤器
 * 负责实施限流策略，保护后端服务免受过度请求的冲击
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {
    
    @Value("${gateway.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${gateway.rate-limit.default.limit:100}")
    private long defaultLimit;
    
    private final RateLimitService rateLimitService;
    private final RequestContextService requestContextService;
    
    /**
     * 过滤器执行顺序（优先级越高，数字越小）
     */
    @Override
    public int getOrder() {
        return 10; // 在认证过滤器之后，API网关过滤器之前
    }
    
    /**
     * 执行限流检查
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 检查限流是否启用
        if (!rateLimitEnabled) {
            return chain.filter(exchange);
        }
        
        // 检查是否在白名单中
        String path = exchange.getRequest().getURI().getPath();
        if (isWhitelistedPath(path)) {
            log.debug("路径在白名单中，跳过限流检查: {}", path);
            return chain.filter(exchange);
        }
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        RequestContext requestContext = requestContextService.getCurrentContext();
        requestContext.markRequestStart();
        
        // 执行多种限流检查
        return executeRateLimitChecks(exchange)
            .flatMap(rateLimitResults -> {
                // 如果任何限流检查失败，返回限流响应
                for (RateLimitResult result : rateLimitResults.values()) {
                    if (!result.isAllowed()) {
                        return handleRateLimitExceeded(exchange, result);
                    }
                }
                
                // 所有限流检查通过，继续处理请求
                return chain.filter(exchange);
            })
            .doOnSuccess(aVoid -> {
                // 记录处理时间
                long duration = System.currentTimeMillis() - startTime;
                requestContext.markRequestSuccess();
                requestContext.setRequestDuration(duration);
                log.debug("限流过滤器处理完成，耗时: {}ms", duration);
            })
            .doOnError(throwable -> {
                // 处理异常情况
                long duration = System.currentTimeMillis() - startTime;
                requestContext.markRequestFailure();
                requestContext.setRequestDuration(duration);
                log.error("限流过滤器处理异常，耗时: {}ms", duration, throwable);
                
                // 限流失败时拒绝请求
                requestContext.setRateLimited(true);
            });
    }
    
    /**
     * 执行多种限流算法检查
     */
    private Mono<Map<String, RateLimitResult>> executeRateLimitChecks(ServerWebExchange exchange) {
        // 获取请求信息
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() != null ? 
            exchange.getRequest().getMethod().name() : "GET";
        
        // 获取客户端信息和限流配置
        ClientInfo clientInfo = extractClientInfo(exchange);
        RequestContext requestContext = requestContextService.getCurrentContext();
        requestContext.setClientInfo(clientInfo);
        
        // 获取限流配置
        RateLimitConfig rateLimitConfig = rateLimitService.getRateLimitConfig(exchange, path);
        
        if (!rateLimitConfig.isEnableRateLimit()) {
            Map<String, RateLimitResult> results = new HashMap<>();
            results.put("global", RateLimitResult.success(-1, -1));
            return Mono.just(results);
        }
        
        // 执行并行限流检查
        Map<String, Mono<RateLimitResult>> checks = new HashMap<>();
        
        // IP限流
        checks.put("ip", checkIpRateLimit(exchange, clientInfo, rateLimitConfig));
        
        // 用户限流（如果有用户信息）
        if (requestContext.getAuthInfo() != null && requestContext.getAuthInfo().getUserId() != null) {
            checks.put("user", checkUserRateLimit(exchange, requestContext.getAuthInfo().getUserId(), rateLimitConfig));
        }
        
        // API限流
        checks.put("api", checkApiRateLimit(exchange, path, method, rateLimitConfig));
        
        // 租户限流（如果有租户信息）
        if (requestContext.getAuthInfo() != null && requestContext.getAuthInfo().getTenantId() != null) {
            checks.put("tenant", checkTenantRateLimit(exchange, requestContext.getAuthInfo().getTenantId(), rateLimitConfig));
        }
        
        // 全局限流
        checks.put("global", checkGlobalRateLimit(exchange, rateLimitConfig));
        
        // 并行执行所有限流检查
        return Mono.when(checks.values().toArray(new Mono[0]))
            .map(results -> {
                Map<String, RateLimitResult> resultMap = new HashMap<>();
                int index = 0;
                for (String key : checks.keySet()) {
                    resultMap.put(key, (RateLimitResult) results[index]);
                    index++;
                }
                return resultMap;
            })
            .timeout(100, TimeUnit.MILLISECONDS) // 限流检查超时限制
            .doOnError(throwable -> {
                log.warn("限流检查超时，继续处理请求: {}", throwable.getMessage());
            })
            .onErrorResume(throwable -> {
                // 超时或异常时，允许请求通过，但记录警告
                Map<String, RateLimitResult> defaultResults = new HashMap<>();
                defaultResults.put("global", RateLimitResult.success(defaultLimit, defaultLimit - 1));
                return Mono.just(defaultResults);
            });
    }
    
    /**
     * IP限流检查
     */
    private Mono<RateLimitResult> checkIpRateLimit(ServerWebExchange exchange, ClientInfo clientInfo, 
                                                  RateLimitConfig config) {
        long limit = config.getIpLimit();
        
        if (clientInfo.isTrustedClient()) {
            // 可信客户端使用更高的限制
            limit = limit * 2;
        }
        
        return rateLimitService.checkIpRateLimit(exchange, limit)
            .doOnNext(result -> {
                result.setLimitType("ip");
                result.setLimitKey(clientInfo.getClientIp());
                result.setClientType(clientInfo.getClientType().toString());
            });
    }
    
    /**
     * 用户限流检查
     */
    private Mono<RateLimitResult> checkUserRateLimit(ServerWebExchange exchange, String userId, 
                                                    RateLimitConfig config) {
        return rateLimitService.checkUserRateLimit(exchange, userId, config.getUserLimit())
            .doOnNext(result -> {
                result.setLimitType("user");
                result.setLimitKey(userId);
                result.setAlgorithmType("sliding_window");
            });
    }
    
    /**
     * API限流检查
     */
    private Mono<RateLimitResult> checkApiRateLimit(ServerWebExchange exchange, String path, String method,
                                                   RateLimitConfig config) {
        // 区分不同方法的API限制
        long baseLimit = config.getApiLimit();
        
        // GET请求通常更频繁
        if ("GET".equalsIgnoreCase(method)) {
            baseLimit = (long) (baseLimit * 1.5);
        } else if ("POST".equalsIgnoreCase(method)) {
            baseLimit = (long) (baseLimit * 0.8); // POST请求较重
        }
        
        return rateLimitService.checkApiRateLimit(exchange, path, baseLimit)
            .doOnNext(result -> {
                result.setLimitType("api");
                result.setLimitKey(method + ":" + path);
                result.setAlgorithmType("token_bucket");
            });
    }
    
    /**
     * 租户限流检查
     */
    private Mono<RateLimitResult> checkTenantRateLimit(ServerWebExchange exchange, String tenantId,
                                                      RateLimitConfig config) {
        return rateLimitService.checkTenantRateLimit(exchange, tenantId, config.getTenantLimit())
            .doOnNext(result -> {
                result.setLimitType("tenant");
                result.setLimitKey(tenantId);
                result.setAlgorithmType("sliding_window");
            });
    }
    
    /**
     * 全局限流检查
     */
    private Mono<RateLimitResult> checkGlobalRateLimit(ServerWebExchange exchange, RateLimitConfig config) {
        return rateLimitService.checkSlidingWindowLimit(
            exchange, "global", 60, config.getGlobalLimit()
        ).doOnNext(result -> {
            result.setLimitType("global");
            result.setLimitKey("system");
            result.setAlgorithmType("sliding_window");
        });
    }
    
    /**
     * 处理限流超限情况
     */
    private Mono<Void> handleRateLimitExceeded(ServerWebExchange exchange, RateLimitResult result) {
        log.warn("限流触发 - 类型: {}, 算法: {}, 消息: {}", 
            result.getLimitType(), result.getAlgorithmType(), result.getMessage());
        
        ServerHttpResponse response = exchange.getResponse();
        
        // 设置限流响应头
        Map<String, String> headers = new HashMap<>();
        result.addHeaders(headers);
        
        headers.forEach((key, value) -> {
            response.getHeaders().set(key, value);
        });
        
        // 设置响应状态和内容
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        
        // 构建响应内容
        String responseBody = String.format(
            "{\"code\": 429, \"message\": \"%s\", \"limitType\": \"%s\", \"algorithm\": \"%s\", \"remaining\": %d, \"resetTime\": %d}",
            escapeJson(result.getMessage()),
            result.getLimitType(),
            result.getAlgorithmType(),
            result.getRemaining(),
            result.getResetTime()
        );
        
        // 记录到监控
        RequestContext context = requestContextService.getCurrentContext();
        context.setRateLimited(true);
        context.setRateLimitInfo(result);
        
        return response.writeWith(
            org.springframework.core.io.buffer.NettyDataBufferFactories
                .defaultAllocators
                .heapBuffer()
                .write(responseBody.getBytes())
                .map(dataBuffer -> responseBody)
        ).then(Mono.empty());
    }
    
    /**
     * 提取客户端信息
     */
    private ClientInfo extractClientInfo(ServerWebExchange exchange) {
        // 简化实现，实际应从请求头等地方提取
        String clientIp = exchange.getRequest().getRemoteAddress() != null ?
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";
            
        return ClientInfo.builder()
            .clientIp(clientIp)
            .userAgent(exchange.getRequest().getHeaders().getFirst("User-Agent"))
            .deviceType(ClientInfo.DeviceType.WEB)
            .networkType(ClientInfo.NetworkType.UNKNOWN)
            .build();
    }
    
    /**
     * 检查是否为白名单路径
     */
    private boolean isWhitelistedPath(String path) {
        // 常见的健康检查和监控端点
        return path.startsWith("/actuator") ||
               path.equals("/health") ||
               path.equals("/metrics") ||
               path.equals("/ping") ||
               path.startsWith("/static/");
    }
    
    /**
     * 转义JSON字符串
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}