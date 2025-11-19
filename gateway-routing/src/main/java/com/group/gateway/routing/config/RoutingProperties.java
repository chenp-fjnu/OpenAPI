package com.group.gateway.routing.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Routing Configuration Properties
 * 路由配置属性类
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "gateway.routing")
@Validated
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RoutingProperties extends BaseGatewayProperties {

    /**
     * Global routing configuration
     * 全局路由配置
     */
    @NotNull
    private GlobalConfig global = new GlobalConfig();

    /**
     * Load balancer configuration
     * 负载均衡配置
     */
    @NotNull
    private LoadBalancerConfig loadBalancer = new LoadBalancerConfig();

    /**
     * Circuit breaker configuration
     * 熔断器配置
     */
    @NotNull
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    /**
     * Rate limiting configuration
     * 限流配置
     */
    @NotNull
    private RateLimitConfig rateLimit = new RateLimitConfig();

    /**
     * Health check configuration
     * 健康检查配置
     */
    @NotNull
    private HealthCheckConfig healthCheck = new HealthCheckConfig();

    /**
     * Dynamic routing configuration
     * 动态路由配置
     */
    @NotNull
    private DynamicRoutingConfig dynamicRouting = new DynamicRoutingConfig();

    /**
     * Global routing configuration
     */
    @Data
    public static class GlobalConfig {
        /**
         * Enable global routing
         * 启用全局路由
         */
        private boolean enabled = true;

        /**
         * Default timeout for route requests (in milliseconds)
         * 路由请求默认超时时间（毫秒）
         */
        private long defaultTimeout = 5000;

        /**
         * Maximum number of concurrent connections per route
         * 每个路由的最大并发连接数
         */
        private int maxConcurrentConnections = 1000;

        /**
         * Request retry configuration
         * 请求重试配置
         */
        private RetryConfig retry = new RetryConfig();

        /**
         * Request/Response transformation configuration
         * 请求/响应转换配置
         */
        private TransformConfig transform = new TransformConfig();

        /**
         * Request retry configuration
         */
        @Data
        public static class RetryConfig {
            private boolean enabled = false;
            private int maxAttempts = 3;
            private List<Integer> retryableStatusCodes;
            private long backoffDelay = 1000;
            private double backoffMultiplier = 2.0;
        }

        /**
         * Request/Response transformation configuration
         */
        @Data
        public static class TransformConfig {
            private RequestConfig request = new RequestConfig();
            private ResponseConfig response = new ResponseConfig();

            @Data
            public static class RequestConfig {
                private boolean addHeaders = true;
                private Map<String, String> headers;
                private boolean preserveHost = false;
            }

            @Data
            public static class ResponseConfig {
                private boolean addHeaders = true;
                private Map<String, String> headers;
                private boolean removeHeaders = false;
                private List<String> headersToRemove;
            }
        }
    }

    /**
     * Load balancer configuration
     */
    @Data
    public static class LoadBalancerConfig {
        /**
         * Load balancing algorithm
         * 负载均衡算法: ROUND_ROBIN, RANDOM, LEAST_CONNECTIONS, WEIGHTED_RESPONSE_TIME
         */
        @NotBlank
        private String algorithm = "ROUND_ROBIN";

        /**
         * Service instance cache configuration
         * 服务实例缓存配置
         */
        private CacheConfig cache = new CacheConfig();

        /**
         * Sticky session configuration
         * 粘性会话配置
         */
        private StickySessionConfig stickySession = new StickySessionConfig();

        /**
         * Health-based load balancing
         * 基于健康状态的负载均衡
         */
        private boolean healthBased = true;

        /**
         * Service instance cache configuration
         */
        @Data
        public static class CacheConfig {
            private boolean enabled = true;
            private long refreshInterval = 30000; // 30 seconds
            private long expireTime = 60000; // 1 minute
        }

        /**
         * Sticky session configuration
         */
        @Data
        public static class StickySessionConfig {
            private boolean enabled = false;
            private String cookieName = "JSESSIONID";
            private int maxAge = 86400; // 24 hours
        }
    }

    /**
     * Circuit breaker configuration
     */
    @Data
    public static class CircuitBreakerConfig {
        /**
         * Enable circuit breaker
         * 启用熔断器
         */
        private boolean enabled = true;

        /**
         * Failure rate threshold (percentage)
         * 故障率阈值（百分比）
         */
        private double failureRateThreshold = 50.0;

        /**
         * Request volume threshold
         * 请求量阈值
         */
        private int requestVolumeThreshold = 20;

        /**
         * Wait duration in open state (seconds)
         * 打开状态等待时间（秒）
         */
        private long waitDurationInOpenState = 60;

        /**
         * Half-open max calls
         * 半开状态最大调用数
         */
        private int halfOpenMaxCalls = 10;

        /**
         * Timeout duration (seconds)
         * 超时时间（秒）
         */
        private long timeoutDuration = 3;

        /**
         * Fallback configuration
         * 降级配置
         */
        private FallbackConfig fallback = new FallbackConfig();

        @Data
        public static class FallbackConfig {
            private boolean enabled = true;
            private String fallbackStrategy = "RETURN_ERROR_RESPONSE"; // RETURN_ERROR_RESPONSE, FORWARD_TO_FALLBACK
            private String fallbackUri;
            private String fallbackMessage = "Service temporarily unavailable";
        }
    }

    /**
     * Rate limiting configuration
     */
    @Data
    public static class RateLimitConfig {
        /**
         * Enable rate limiting
         * 启用限流
         */
        private boolean enabled = false;

        /**
         * Global rate limit per second
         * 全局限流每秒请求数
         */
        private int globalRateLimit = 1000;

        /**
         * Rate limit by IP
         * 按IP限流
         */
        private IpRateLimit ipRateLimit = new IpRateLimit();

        /**
         * Rate limit by user
         * 按用户限流
         */
        private UserRateLimit userRateLimit = new UserRateLimit();

        /**
         * Rate limit by route
         * 按路由限流
         */
        private RouteRateLimit routeRateLimit = new RouteRateLimit();

        @Data
        public static class IpRateLimit {
            private boolean enabled = false;
            private int requestsPerMinute = 60;
            private int requestsPerHour = 1000;
        }

        @Data
        public static class UserRateLimit {
            private boolean enabled = false;
            private int requestsPerMinute = 100;
            private int requestsPerHour = 5000;
        }

        @Data
        public static class RouteRateLimit {
            private boolean enabled = false;
            private Map<String, RouteLimit> limits;

            @Data
            public static class RouteLimit {
                private int requestsPerSecond;
                private int requestsPerMinute;
                private int requestsPerHour;
            }
        }
    }

    /**
     * Health check configuration
     */
    @Data
    public static class HealthCheckConfig {
        /**
         * Enable health check
         * 启用健康检查
         */
        private boolean enabled = true;

        /**
         * Health check interval (seconds)
         * 健康检查间隔（秒）
         */
        private long interval = 30;

        /**
         * Health check timeout (seconds)
         * 健康检查超时时间（秒）
         */
        private long timeout = 5;

        /**
         * Unhealthy threshold
         * 不健康阈值
         */
        private int unhealthyThreshold = 3;

        /**
         * Healthy threshold
         * 健康阈值
         */
        private int healthyThreshold = 2;

        /**
         * Health check path
         * 健康检查路径
         */
        @NotBlank
        private String path = "/actuator/health";

        /**
         * Expected health status
         * 期望的健康状态
         */
        private String expectedStatus = "UP";

        /**
         * Custom health check headers
         * 自定义健康检查头
         */
        private Map<String, String> customHeaders;
    }

    /**
     * Dynamic routing configuration
     */
    @Data
    public static class DynamicRoutingConfig {
        /**
         * Enable dynamic routing
         * 启用动态路由
         */
        private boolean enabled = true;

        /**
         * Auto-refresh routes interval (seconds)
         * 自动刷新路由间隔（秒）
         */
        private long autoRefreshInterval = 60;

        /**
         * Route validation enabled
         * 启用路由验证
         */
        private boolean validateRoutes = true;

        /**
         * Route storage configuration
         * 路由存储配置
         */
        private StorageConfig storage = new StorageConfig();

        @Data
        public static class StorageConfig {
            /**
             * Storage type: MEMORY, REDIS, DATABASE
             * 存储类型
             */
            @NotBlank
            private String type = "MEMORY";

            /**
             * Redis configuration for route storage
             * Redis路由存储配置
             */
            private RedisConfig redis = new RedisConfig();

            /**
             * Database configuration for route storage
             * 数据库路由存储配置
             */
            private DatabaseConfig database = new DatabaseConfig();

            @Data
            public static class RedisConfig {
                private String keyPrefix = "gateway:routes:";
                private long expireTime = 3600; // 1 hour
            }

            @Data
            public static class DatabaseConfig {
                private String tableName = "gateway_routes";
                private boolean autoCreateTable = true;
            }
        }
    }
}