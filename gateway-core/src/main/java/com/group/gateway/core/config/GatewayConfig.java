package com.group.gateway.core.config;

import com.group.gateway.core.filter.GlobalRequestFilter;
import com.group.gateway.core.filter.GlobalResponseFilter;
import com.group.gateway.core.filter.AuthFilter;
import com.group.gateway.core.filter.RateLimitFilter;
import com.group.gateway.core.filter.CircuitBreakerFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.factory.RequestRateLimiterGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.RetryGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 网关核心配置类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {
    
    @Value("${gateway.rate.limit.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${gateway.circuit.breaker.enabled:true}")
    private boolean circuitBreakerEnabled;
    
    @Value("${gateway.auth.enabled:true}")
    private boolean authEnabled;
    
    /**
     * 自定义路由配置
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 公共API路由
            .route("public_api", r -> r
                .path("/api/public/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(ipKeyResolver())
                        .setStatusCode(429)
                    )
                    .circuitBreaker(config -> config
                        .setName("publicApiCircuitBreaker")
                        .setFallbackUri("forward:/fallback/public")
                        .setTimeoutDuration(5000)
                    )
                )
                .uri("lb://public-service")
            )
            
            // 私有API路由
            .route("private_api", r -> r
                .path("/api/private/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())
                        .setStatusCode(429)
                    )
                    .circuitBreaker(config -> config
                        .setName("privateApiCircuitBreaker")
                        .setFallbackUri("forward:/fallback/private")
                        .setTimeoutDuration(3000)
                    )
                )
                .uri("lb://private-service")
            )
            
            // 管理API路由
            .route("admin_api", r -> r
                .path("/api/admin/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(userKeyResolver())
                        .setStatusCode(429)
                    )
                )
                .uri("lb://admin-service")
            )
            
            // 实时数据API路由
            .route("realtime_api", r -> r
                .path("/api/realtime/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .requestRateLimiter(config -> config
                        .setRateLimiter(redisRateLimiter())
                        .setKeyResolver(clientKeyResolver())
                        .setStatusCode(429)
                    )
                    .retry(config -> config
                        .setRetries(2)
                        .setSeries(Arrays.asList(HttpMethod.INTERNAL_SERVER_ERROR, 
                                                HttpMethod.BAD_GATEWAY, 
                                                HttpMethod.SERVICE_UNAVAILABLE))
                    )
                )
                .uri("lb://realtime-service")
            )
            .build();
    }
    
    /**
     * IP限流解析器
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress()
                .map(address -> address.getAddress().getHostAddress())
                .defaultIfEmpty("unknown")
        );
    }
    
    /**
     * 用户限流解析器
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders()
                .getFirst("X-User-ID");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
    
    /**
     * 客户端限流解析器
     */
    @Bean("clientKeyResolver")
    public KeyResolver clientKeyResolver() {
        return exchange -> {
            String clientId = exchange.getRequest().getHeaders()
                .getFirst("X-Client-ID");
            String userAgent = exchange.getRequest().getHeaders()
                .getFirst("User-Agent");
            return Mono.just((clientId != null ? clientId : "unknown") + "-" + 
                           (userAgent != null ? userAgent.hashCode() : "0"));
        };
    }
    
    /**
     * Redis限流器配置
     */
    @Bean
    public RequestRateLimiterGatewayFilterFactory redisRateLimiter() {
        return new RequestRateLimiterGatewayFilterFactory();
    }
    
    /**
     * 全局请求过滤器
     */
    @Bean
    public GlobalRequestFilter globalRequestFilter() {
        return new GlobalRequestFilter();
    }
    
    /**
     * 全局响应过滤器
     */
    @Bean
    public GlobalResponseFilter globalResponseFilter() {
        return new GlobalResponseFilter();
    }
    
    /**
     * 认证过滤器
     */
    @Bean
    public AuthFilter authFilter() {
        return new AuthFilter();
    }
    
    /**
     * 限流过滤器
     */
    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }
    
    /**
     * 熔断器过滤器
     */
    @Bean
    public CircuitBreakerFilter circuitBreakerFilter() {
        return new CircuitBreakerFilter();
    }
    
    /**
     * 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 网关属性配置
     */
    @Bean
    public GatewayProperties gatewayProperties() {
        return new GatewayProperties();
    }
}