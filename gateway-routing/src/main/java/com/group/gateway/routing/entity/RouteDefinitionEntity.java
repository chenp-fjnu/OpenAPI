package com.group.gateway.routing.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.cloud.gateway.route.RouteDefinition;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Route Definition Entity
 * 路由定义实体类
 */
@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RouteDefinitionEntity extends RouteDefinition {

    /**
     * Route ID
     */
    @NotBlank
    private String routeId;

    /**
     * Route description
     * 路由描述
     */
    private String description;

    /**
     * Route status: ACTIVE, INACTIVE, DISABLED
     * 路由状态
     */
    @NotNull
    private RouteStatus status = RouteStatus.ACTIVE;

    /**
     * Route priority (lower number = higher priority)
     * 路由优先级（数字越小优先级越高）
     */
    private int priority = 100;

    /**
     * Route tags for categorization
     * 路由标签，用于分类
     */
    private List<String> tags;

    /**
     * Route metadata
     * 路由元数据
     */
    private Map<String, Object> metadata;

    /**
     * Load balancer configuration for this route
     * 此路由的负载均衡器配置
     */
    @Valid
    private LoadBalancerConfig loadBalancerConfig;

    /**
     * Circuit breaker configuration for this route
     * 此路由的熔断器配置
     */
    @Valid
    private CircuitBreakerConfig circuitBreakerConfig;

    /**
     * Rate limiting configuration for this route
     * 此路由的限流配置
     */
    @Valid
    private RateLimitConfig rateLimitConfig;

    /**
     * Created time
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * Updated time
     * 更新时间
     */
    private LocalDateTime updatedTime;

    /**
     * Created by user
     * 创建用户
     */
    private String createdBy;

    /**
     * Updated by user
     * 更新用户
     */
    private String updatedBy;

    /**
     * Route status enumeration
     */
    public enum RouteStatus {
        ACTIVE,    // 活跃
        INACTIVE,  // 非活跃
        DISABLED,  // 禁用
        MAINTENANCE // 维护中
    }

    /**
     * Load balancer configuration for route
     */
    @Data
    public static class LoadBalancerConfig {
        /**
         * Load balancing algorithm
         * 负载均衡算法
         */
        @NotBlank
        private String algorithm = "ROUND_ROBIN";

        /**
         * Sticky session enabled
         * 启用粘性会话
         */
        private boolean stickySession = false;

        /**
         * Sticky session cookie name
         * 粘性会话Cookie名称
         */
        private String cookieName = "JSESSIONID";

        /**
         * Instance weight configuration
         * 实例权重配置
         */
        private Map<String, Double> instanceWeights;
    }

    /**
     * Circuit breaker configuration for route
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * Enable circuit breaker for this route
         * 为此路由启用熔断器
         */
        private boolean enabled = false;

        /**
         * Failure rate threshold (percentage)
         * 故障率阈值（百分比）
         */
        private Double failureRateThreshold = 50.0;

        /**
         * Request volume threshold
         * 请求量阈值
         */
        private Integer requestVolumeThreshold = 20;

        /**
         * Wait duration in open state (seconds)
         * 打开状态等待时间（秒）
         */
        private Long waitDurationInOpenState = 60L;

        /**
         * Fallback strategy
         * 降级策略
         */
        private String fallbackStrategy = "RETURN_ERROR_RESPONSE";

        /**
         * Fallback URI
         * 降级URI
         */
        private String fallbackUri;

        /**
         * Fallback message
         * 降级消息
         */
        private String fallbackMessage = "Service temporarily unavailable";
    }

    /**
     * Rate limiting configuration for route
     */
    @Data
    public static class RateLimitConfig {
        /**
         * Enable rate limiting for this route
         * 为此路由启用限流
         */
        private boolean enabled = false;

        /**
         * Requests per second
         * 每秒请求数
         */
        private Integer requestsPerSecond;

        /**
         * Requests per minute
         * 每分钟请求数
         */
        private Integer requestsPerMinute;

        /**
         * Requests per hour
         * 每小时请求数
         */
        private Integer requestsPerHour;

        /**
         * Rate limit key resolver
         * 限流键解析器：IP, USER, API_KEY, CUSTOM
         */
        private String keyResolver = "IP";

        /**
         * Custom rate limit key expression
         * 自定义限流键表达式
         */
        private String customKeyExpression;
    }
}