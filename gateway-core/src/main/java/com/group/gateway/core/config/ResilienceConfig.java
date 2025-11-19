package com.group.gateway.core.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * 熔断器配置类
 * 负责配置 Resilience4j 熔断器的各种参数和策略
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Configuration
public class ResilienceConfig {
    
    @Value("${gateway.circuitbreaker.failureRateThreshold:50}")
    private double failureRateThreshold;
    
    @Value("${gateway.circuitbreaker.waitDurationInOpenState:30s}")
    private String waitDurationInOpenState;
    
    @Value("${gateway.circuitbreaker.slidingWindowSize:10}")
    private int slidingWindowSize;
    
    @Value("${gateway.circuitbreaker.minimumNumberOfCalls:5}")
    private int minimumNumberOfCalls;
    
    @Value("${gateway.circuitbreaker.permittedNumberOfCallsInHalfOpenState:3}")
    private int permittedNumberOfCallsInHalfOpenState;
    
    @Value("${gateway.retry.maxAttempts:3}")
    private int maxAttempts;
    
    @Value("${gateway.retry.backoff.initialInterval:100ms}")
    private String initialInterval;
    
    @Value("${gateway.retry.backoff.multiplier:2}")
    private double multiplier;
    
    @Value("${gateway.retry.backoff.maxInterval:1000ms}")
    private String maxInterval;
    
    @Value("${gateway.performance.timeouts.connect:5000ms}")
    private String connectTimeout;
    
    @Value("${gateway.performance.timeouts.read:10000ms}")
    private String readTimeout;
    
    @Value("${gateway.performance.timeouts.write:10000ms}")
    private String writeTimeout;
    
    /**
     * 配置默认的熔断器
     */
    @Bean
    @Primary
    public CircuitBreakerFactory<?, ?> circuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry, timeLimiterRegistry, null);
        
        // 配置默认的熔断器
        factory.configureDefault(id -> {
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    // 失败率阈值：50%
                    .failureRateThreshold(failureRateThreshold)
                    // 慢调用阈值：60%
                    .slowCallRateThreshold(60.0f)
                    // 慢调用持续时间：2秒
                    .slowCallDurationThreshold(Duration.ofSeconds(2))
                    // 滑动窗口大小：10个请求
                    .slidingWindowSize(slidingWindowSize)
                    // 最小调用次数：5次
                    .minimumNumberOfCalls(minimumNumberOfCalls)
                    // 半开状态允许调用次数：3次
                    .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
                    // 等待状态持续时间：30秒
                    .waitDurationInOpenState(Duration.parse(waitDurationInOpenState))
                    // 事件缓冲区大小：100
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                    .build();
            
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    // 超时时间：10秒
                    .timeoutDuration(Duration.parse(readTimeout))
                    .build();
            
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build();
        });
        
        return factory;
    }
    
    /**
     * 配置公共API的熔断器
     */
    @Bean
    public CircuitBreakerFactory<?, ?> publicApiCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry, timeLimiterRegistry, null);
        
        factory.configureDefault(id -> {
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(40.0f) // 更低的失败率阈值
                    .slowCallRateThreshold(50.0f)
                    .slowCallDurationThreshold(Duration.ofSeconds(3))
                    .slidingWindowSize(20) // 更大的滑动窗口
                    .minimumNumberOfCalls(8)
                    .permittedNumberOfCallsInHalfOpenState(5)
                    .waitDurationInOpenState(Duration.ofSeconds(20))
                    .build();
            
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.parse(connectTimeout))
                    .build();
            
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build();
        });
        
        return factory;
    }
    
    /**
     * 配置管理API的熔断器（更严格的配置）
     */
    @Bean
    public CircuitBreakerFactory<?, ?> adminApiCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry, timeLimiterRegistry, null);
        
        factory.configureDefault(id -> {
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(30.0f) // 更严格的失败率阈值
                    .slowCallRateThreshold(40.0f)
                    .slowCallDurationThreshold(Duration.ofSeconds(1))
                    .slidingWindowSize(5) // 更小的滑动窗口
                    .minimumNumberOfCalls(3)
                    .permittedNumberOfCallsInHalfOpenState(1)
                    .waitDurationInOpenState(Duration.ofSeconds(60))
                    .build();
            
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.parse(connectTimeout))
                    .build();
            
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build();
        });
        
        return factory;
    }
    
    /**
     * 配置实时数据API的熔断器（更宽松的配置）
     */
    @Bean
    public CircuitBreakerFactory<?, ?> realtimeApiCircuitBreakerFactory(
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry, timeLimiterRegistry, null);
        
        factory.configureDefault(id -> {
            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(70.0f) // 更宽松的失败率阈值
                    .slowCallRateThreshold(80.0f)
                    .slowCallDurationThreshold(Duration.ofSeconds(5))
                    .slidingWindowSize(50) // 更大的滑动窗口
                    .minimumNumberOfCalls(20)
                    .permittedNumberOfCallsInHalfOpenState(10)
                    .waitDurationInOpenState(Duration.ofSeconds(10))
                    .build();
            
            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(30)) // 更长的超时时间
                    .build();
            
            return new Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(circuitBreakerConfig)
                    .timeLimiterConfig(timeLimiterConfig)
                    .build();
        });
        
        return factory;
    }
    
    /**
     * 配置重试策略
     */
    @Bean
    public io.github.resilience4j.retry.RetryConfig<?> retryConfig() {
        return io.github.resilience4j.retry.RetryConfig.<String>custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.parse(initialInterval))
                .retryExceptions(
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class,
                        org.springframework.web.client.RestClientException.class
                )
                .build();
    }
    
    /**
     * 配置熔断器监控
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        return TimeLimiterRegistry.ofDefaults();
    }
}