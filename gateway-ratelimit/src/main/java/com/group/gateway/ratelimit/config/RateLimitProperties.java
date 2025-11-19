package com.group.gateway.ratelimit.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 限流模块配置属性
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "gateway.ratelimit")
public class RateLimitProperties {

    /**
     * 是否启用限流功能
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 默认限流配置
     */
    private DefaultConfig defaultConfig;

    /**
     * 限流算法配置
     */
    private AlgorithmConfig algorithms;

    /**
     * Redis配置
     */
    private RedisConfig redis;

    /**
     * 监控配置
     */
    private MonitorConfig monitor;

    /**
     * 策略配置映射
     */
    private Map<String, StrategyConfig> strategies;

    /**
     * 降级配置
     */
    private FallbackConfig fallback;

    /**
     * 默认配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultConfig {
        /**
         * 默认限流算法类型
         */
        @NotBlank(message = "默认限流算法类型不能为空")
        @Builder.Default
        private String algorithm = "token-bucket";

        /**
         * 默认请求速率（每秒请求数）
         */
        @NotNull(message = "默认请求速率不能为空")
        @Builder.Default
        private double rate = 100.0;

        /**
         * 默认突发容量
         */
        @NotNull(message = "默认突发容量不能为空")
        @Builder.Default
        private long burstCapacity = 200L;

        /**
         * 默认响应状态码
         */
        @Builder.Default
        private int responseStatusCode = 429;

        /**
         * 默认响应消息
         */
        @Builder.Default
        private String responseMessage = "请求过于频繁，请稍后再试";
    }

    /**
     * 算法配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmConfig {
        /**
         * 令牌桶算法配置
         */
        private TokenBucketConfig tokenBucket;

        /**
         * 滑动窗口算法配置
         */
        private SlidingWindowConfig slidingWindow;

        /**
         * 固定窗口算法配置
         */
        private FixedWindowConfig fixedWindow;
    }

    /**
     * 令牌桶算法配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenBucketConfig {
        /**
         * 令牌生成速率（每秒生成令牌数）
         */
        @Builder.Default
        private double refillTokens = 10.0;

        /**
         * 令牌生成间隔（纳秒）
         */
        @Builder.Default
        private long refillPeriodNanos = 1_000_000_000L;
    }

    /**
     * 滑动窗口算法配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlidingWindowConfig {
        /**
         * 窗口大小（秒）
         */
        @Builder.Default
        private int windowSize = 60;
    }

    /**
     * 固定窗口算法配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FixedWindowConfig {
        /**
         * 窗口大小（秒）
         */
        @Builder.Default
        private int windowSize = 60;
    }

    /**
     * Redis配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisConfig {
        /**
         * Redis键前缀
         */
        @Builder.Default
        private String keyPrefix = "gateway:ratelimit:";

        /**
         * 键过期时间（秒）
         */
        @Builder.Default
        private long keyExpireSeconds = 300;

        /**
         * Lua脚本存储键前缀
         */
        @Builder.Default
        private String scriptKeyPrefix = "gateway:ratelimit:script:";
    }

    /**
     * 监控配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitorConfig {
        /**
         * 是否启用监控
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * 监控指标名称前缀
         */
        @Builder.Default
        private String metricsPrefix = "gateway_ratelimit";

        /**
         * 指标维度
         */
        @Builder.Default
        private List<String> dimensions = List.of("strategy", "client_ip", "user_id", "api_path");
    }

    /**
     * 策略配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyConfig {
        /**
         * 策略名称
         */
        private String name;

        /**
         * 策略描述
         */
        private String description;

        /**
         * 算法类型
         */
        @NotBlank(message = "策略算法类型不能为空")
        private String algorithm;

        /**
         * 限流参数
         */
        private LimitParams limitParams;

        /**
         * 限流维度
         */
        @NotEmpty(message = "限流维度不能为空")
        private List<String> dimensions;

        /**
         * 是否启用
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * 优先级（数值越小优先级越高）
         */
        @Builder.Default
        private int priority = 100;
    }

    /**
     * 限流参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LimitParams {
        /**
         * 请求速率
         */
        private double rate;

        /**
         * 突发容量
         */
        private long capacity;

        /**
         * 时间窗口大小（秒）
         */
        private int windowSize;
    }

    /**
     * 降级配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FallbackConfig {
        /**
         * 是否启用降级
         */
        @Builder.Default
        private boolean enabled = false;

        /**
         * 降级策略类型：reject-拒绝，delay-延迟，mock-模拟
         */
        @Builder.Default
        private String strategy = "reject";

        /**
         * 延迟时间（毫秒）
         */
        @Builder.Default
        private long delayMillis = 1000;

        /**
         * 降级响应状态码
         */
        @Builder.Default
        private int responseStatusCode = 503;

        /**
         * 降级响应消息
         */
        @Builder.Default
        private String responseMessage = "服务繁忙，请稍后重试";
    }
}