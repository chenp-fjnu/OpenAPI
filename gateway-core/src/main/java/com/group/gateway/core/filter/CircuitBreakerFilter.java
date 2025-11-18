package com.group.gateway.core.filter;

import com.group.gateway.core.service.RequestContextService;
import com.group.gateway.core.service.RequestTraceService;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

/**
 * 熔断器过滤器
 * 负责为每个请求应用熔断器、重试和超时保护
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class CircuitBreakerFilter implements GlobalFilter, Ordered {
    
    @Autowired
    private RequestContextService requestContextService;
    
    @Autowired
    private RequestTraceService requestTraceService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Autowired
    private RetryConfig<String> retryConfig;
    
    @Autowired
    @Qualifier("circuitBreakerFactory")
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    
    private static final String CIRCUIT_BREAKER_KEY_PREFIX = "gateway-circuit-";
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = requestContextService.getTraceId(exchange);
        String path = exchange.getRequest().getURI().getPath();
        
        // 选择合适的熔断器工厂
        CircuitBreakerFactory<?, ?> selectedFactory = selectCircuitBreakerFactory(path);
        
        return Mono.defer(() -> {
            try {
                // 创建熔断器实例
                String circuitBreakerName = createCircuitBreakerName(path);
                CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(circuitBreakerName);
                
                // 执行熔断器保护的业务逻辑
                return executeWithCircuitBreakerProtection(exchange, chain, circuitBreaker, selectedFactory);
                
            } catch (Exception e) {
                log.error("熔断器执行异常 - traceId: {}, path: {}", traceId, path, e);
                return handleCircuitBreakerError(exchange, e);
            }
        });
    }
    
    /**
     * 执行熔断器保护的业务逻辑
     */
    private Mono<Void> executeWithCircuitBreakerProtection(ServerWebExchange exchange, 
                                                          GatewayFilterChain chain, 
                                                          CircuitBreaker circuitBreaker,
                                                          CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        String traceId = requestContextService.getTraceId(exchange);
        String path = exchange.getRequest().getURI().getPath();
        
        // 创建可重试的函数
        Function<ServerWebExchange, Mono<Void>> decorateSupplier = 
                circuitBreaker.decorateSupplier(() -> {
                    log.debug("熔断器执行中 - traceId: {}, path: {}, state: {}", 
                            traceId, path, circuitBreaker.getState());
                    return chain.filter(exchange);
                });
        
        // 执行函数
        return ((Function<ServerWebExchange, Mono<Void>>) decorateSupplier).apply(exchange)
                .onErrorResume(throwable -> {
                    log.warn("熔断器触发降级 - traceId: {}, path: {}, state: {}, error: {}", 
                            traceId, path, circuitBreaker.getState(), throwable.getMessage());
                    
                    // 记录降级事件
                    recordCircuitBreakerEvent(traceId, path, circuitBreaker.getState(), throwable);
                    
                    // 返回降级响应
                    return handleCircuitBreakerOpen(exchange, circuitBreaker);
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(throwable -> {
                    log.error("熔断器超时 - traceId: {}, path: {}", traceId, path, throwable);
                    return handleCircuitBreakerTimeout(exchange, throwable);
                });
    }
    
    /**
     * 获取或创建熔断器
     */
    private CircuitBreaker getOrCreateCircuitBreaker(String name) {
        try {
            return circuitBreakerRegistry.circuitBreaker(name);
        } catch (Exception e) {
            log.warn("熔断器创建失败，使用默认配置: {}", name, e);
            return CircuitBreaker.ofDefaults(name);
        }
    }
    
    /**
     * 创建熔断器名称
     */
    private String createCircuitBreakerName(String path) {
        return CIRCUIT_BREAKER_KEY_PREFIX + path.replace("/", "_").replace("-", "_");
    }
    
    /**
     * 选择合适的熔断器工厂
     */
    private CircuitBreakerFactory<?, ?> selectCircuitBreakerFactory(String path) {
        if (path.startsWith("/api/v1/admin")) {
            return circuitBreakerFactory;
        } else if (path.startsWith("/api/v1/realtime")) {
            return circuitBreakerFactory;
        } else if (path.startsWith("/api/v1/private")) {
            return circuitBreakerFactory;
        } else {
            return circuitBreakerFactory;
        }
    }
    
    /**
     * 处理熔断器开启状态
     */
    private Mono<Void> handleCircuitBreakerOpen(ServerWebExchange exchange, CircuitBreaker circuitBreaker) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"error\":\"Service temporarily unavailable\",\"message\":\"Circuit breaker is open\",\"retryAfter\":\"" + 
                         circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState().getSeconds() + 
                         "\"}").getBytes()
                ))
        );
    }
    
    /**
     * 处理熔断器错误
     */
    private Mono<Void> handleCircuitBreakerError(ServerWebExchange exchange, Throwable throwable) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"error\":\"Internal server error\",\"message\":\"Circuit breaker execution failed\"}").getBytes()
                ))
        );
    }
    
    /**
     * 处理熔断器超时
     */
    private Mono<Void> handleCircuitBreakerTimeout(ServerWebExchange exchange, Throwable throwable) {
        exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(
                        ("{\"error\":\"Gateway timeout\",\"message\":\"Request timeout\"}").getBytes()
                ))
        );
    }
    
    /**
     * 记录熔断器事件
     */
    private void recordCircuitBreakerEvent(String traceId, String path, 
                                          CircuitBreaker.State state, Throwable error) {
        try {
            String event = "CIRCUIT_BREAKER_" + state;
            log.info("熔断器事件 - traceId: {}, path: {}, event: {}, error: {}", 
                    traceId, path, event, error.getMessage());
            
            // 这里可以发送到监控系统
            // monitorService.recordCircuitBreakerEvent(event, path, state, error);
            
        } catch (Exception e) {
            log.error("记录熔断器事件异常", e);
        }
    }
    
    /**
     * 获取熔断器状态
     */
    public CircuitBreaker.State getCircuitBreakerState(String path) {
        try {
            String name = createCircuitBreakerName(path);
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            return circuitBreaker.getState();
        } catch (Exception e) {
            log.error("获取熔断器状态异常", e);
            return CircuitBreaker.State.CLOSED;
        }
    }
    
    /**
     * 获取熔断器统计信息
     */
    public Map<String, Object> getCircuitBreakerStatistics(String path) {
        try {
            String name = createCircuitBreakerName(path);
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
            
            Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("name", name);
            stats.put("state", circuitBreaker.getState().name());
            stats.put("failureRate", circuitBreaker.getFailureRate());
            stats.put("slowCallRate", circuitBreaker.getSlowCallRate());
            stats.put("numberOfBufferedCalls", circuitBreaker.getMetrics().getNumberOfBufferedCalls());
            stats.put("numberOfSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
            stats.put("numberOfFailedCalls", circuitBreaker.getMetrics().getNumberOfFailedCalls());
            stats.put("numberOfSlowCalls", circuitBreaker.getMetrics().getNumberOfSlowCalls());
            stats.put("numberOfSlowSuccessfulCalls", circuitBreaker.getMetrics().getNumberOfSlowSuccessfulCalls());
            stats.put("numberOfSlowFailedCalls", circuitBreaker.getMetrics().getNumberOfSlowFailedCalls());
            stats.put("numberOfNotPermittedCalls", circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
            
            return stats;
        } catch (Exception e) {
            log.error("获取熔断器统计信息异常", e);
            return new java.util.HashMap<>();
        }
    }
    
    @Override
    public int getOrder() {
        return 80; // 在限流过滤器之后，在认证过滤器之前
    }
}