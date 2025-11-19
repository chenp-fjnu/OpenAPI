package com.group.gateway.monitor.config;

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
 * 监控模块配置属性
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "gateway.monitor")
public class MonitorProperties {

    /**
     * 是否启用监控功能
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 监控配置
     */
    private MonitoringConfig monitoring;

    /**
     * 指标配置
     */
    private MetricsConfig metrics;

    /**
     * 告警配置
     */
    private AlertingConfig alerting;

    /**
     * 存储配置
     */
    private StorageConfig storage;

    /**
     * 链路追踪配置
     */
    private TracingConfig tracing;

    /**
     * 面板配置
     */
    private DashboardConfig dashboard;

    /**
     * 监控配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonitoringConfig {
        /**
         * 采样率（0.0-1.0）
         */
        @Builder.Default
        private double samplingRate = 1.0;

        /**
         * 监控维度
         */
        @Builder.Default
        private List<String> dimensions = List.of("service", "method", "status_code", "client_ip");

        /**
         * 性能指标收集间隔（毫秒）
         */
        @Builder.Default
        private long metricsCollectionInterval = 60000;

        /**
         * 是否启用实时监控
         */
        @Builder.Default
        private boolean realTimeMonitoring = true;

        /**
         * 是否启用历史数据分析
         */
        @Builder.Default
        private boolean historicalAnalysis = true;
    }

    /**
     * 指标配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricsConfig {
        /**
         * 指标名称前缀
         */
        @Builder.Default
        private String prefix = "gateway_monitor";

        /**
         * 是否启用Prometheus集成
         */
        @Builder.Default
        private boolean prometheusEnabled = true;

        /**
         * Prometheus端点路径
         */
        @Builder.Default
        private String prometheusEndpoint = "/actuator/prometheus";

        /**
         * 指标收集器配置
         */
        private CollectorConfig collector;

        /**
         * 请求指标配置
         */
        private RequestMetricsConfig requestMetrics;

        /**
         * 性能指标配置
         */
        private PerformanceMetricsConfig performanceMetrics;

        /**
         * 业务指标配置
         */
        private BusinessMetricsConfig businessMetrics;

        /**
         * 收集器配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CollectorConfig {
            /**
             * 是否启用并发指标收集
             */
            @Builder.Default
            private boolean concurrentCollection = true;

            /**
             * 收集器线程池大小
             */
            @Builder.Default
            private int threadPoolSize = 10;

            /**
             * 指标数据缓冲大小
             */
            @Builder.Default
            private int bufferSize = 10000;
        }

        /**
         * 请求指标配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RequestMetricsConfig {
            /**
             * 是否启用请求计数
             */
            @Builder.Default
            private boolean requestCountEnabled = true;

            /**
             * 是否启用响应时间统计
             */
            @Builder.Default
            private boolean responseTimeEnabled = true;

            /**
             * 是否启用错误率统计
             */
            @Builder.Default
            private boolean errorRateEnabled = true;

            /**
             * 响应时间直方图桶
             */
            @Builder.Default
            private List<String> responseTimeBuckets = List.of("0.1", "0.5", "1.0", "2.0", "5.0");
        }

        /**
         * 性能指标配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PerformanceMetricsConfig {
            /**
             * 是否启用JVM指标
             */
            @Builder.Default
            private boolean jvmMetricsEnabled = true;

            /**
             * 是否启用系统指标
             */
            @Builder.Default
            private boolean systemMetricsEnabled = true;

            /**
             * 是否启用应用指标
             */
            @Builder.Default
            private boolean applicationMetricsEnabled = true;
        }

        /**
         * 业务指标配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BusinessMetricsConfig {
            /**
             * 是否启用用户行为分析
             */
            @Builder.Default
            private boolean userBehaviorAnalysis = true;

            /**
             * 是否启用流量分析
             */
            @Builder.Default
            private boolean trafficAnalysis = true;

            /**
             * 业务指标维度
             */
            @Builder.Default
            private List<String> businessDimensions = List.of("user_type", "api_category", "access_pattern");
        }
    }

    /**
     * 告警配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertingConfig {
        /**
         * 是否启用告警
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * 告警规则配置
         */
        private List<AlertRuleConfig> rules;

        /**
         * 告警渠道配置
         */
        private ChannelsConfig channels;

        /**
         * 告警抑制配置
         */
        private SuppressionConfig suppression;

        /**
         * 告警规则配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AlertRuleConfig {
            /**
             * 规则名称
             */
            @NotBlank(message = "告警规则名称不能为空")
            private String name;

            /**
             * 规则描述
             */
            private String description;

            /**
             * 告警级别：INFO、WARN、ERROR、CRITICAL
             */
            @NotBlank(message = "告警级别不能为空")
            private String level;

            /**
             * 指标名称
             */
            @NotBlank(message = "指标名称不能为空")
            private String metricName;

            /**
             * 阈值条件
             */
            @NotBlank(message = "阈值条件不能为空")
            private String threshold;

            /**
             * 持续时间（秒）
             */
            @Builder.Default
            private long duration = 300;

            /**
             * 是否启用
             */
            @Builder.Default
            private boolean enabled = true;
        }

        /**
         * 告警渠道配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ChannelsConfig {
            /**
             * 邮件告警配置
             */
            private EmailConfig email;

            /**
             * Webhook告警配置
             */
            private WebhookConfig webhook;

            /**
             * 短信告警配置
             */
            private SmsConfig sms;
        }

        /**
         * 告警抑制配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SuppressionConfig {
            /**
             * 是否启用告警抑制
             */
            @Builder.Default
            private boolean enabled = false;

            /**
             * 抑制时间（秒）
             */
            @Builder.Default
            private long suppressDuration = 1800;

            /**
             * 抑制规则
             */
            @Builder.Default
            private List<String> suppressRules = List.of("same_alert_same_source");
        }
    }

    /**
     * 存储配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageConfig {
        /**
         * 存储类型：MEMORY、REDIS、INFLUXDB、TIMESCALEDB
         */
        @NotBlank(message = "存储类型不能为空")
        @Builder.Default
        private String type = "REDIS";

        /**
         * Redis配置
         */
        private RedisConfig redis;

        /**
         * InfluxDB配置
         */
        private InfluxDbConfig influxDb;

        /**
         * 数据保留时间（天）
         */
        @Builder.Default
        private int retentionDays = 30;

        /**
         * 数据压缩配置
         */
        private CompressionConfig compression;

        /**
         * Redis配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RedisConfig {
            /**
             * 键前缀
             */
            @Builder.Default
            private String keyPrefix = "gateway:monitor:";

            /**
             * 数据过期时间（秒）
             */
            @Builder.Default
            private long expireSeconds = 86400;
        }

        /**
         * InfluxDB配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InfluxDbConfig {
            /**
             * 服务器地址
             */
            @NotBlank(message = "InfluxDB服务器地址不能为空")
            private String serverUrl;

            /**
             * 数据库名称
             */
            @NotBlank(message = "InfluxDB数据库名称不能为空")
            private String database;

            /**
             * 用户名
             */
            private String username;

            /**
             * 密码
             */
            private String password;
        }

        /**
         * 数据压缩配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CompressionConfig {
            /**
             * 是否启用压缩
             */
            @Builder.Default
            private boolean enabled = false;

            /**
             * 压缩算法：GZIP、LZ4、SNAPPY
             */
            @Builder.Default
            private String algorithm = "GZIP";
        }
    }

    /**
     * 链路追踪配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TracingConfig {
        /**
         * 是否启用链路追踪
         */
        @Builder.Default
        private boolean enabled = false;

        /**
         * 采样率（0.0-1.0）
         */
        @Builder.Default
        private double samplingRate = 0.1;

        /**
         * 追踪ID头部名称
         */
        @Builder.Default
        private String traceIdHeader = "X-Trace-Id";

        /**
         * Span ID头部名称
         */
        @Builder.Default
        private String spanIdHeader = "X-Span-Id";
    }

    /**
     * 面板配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardConfig {
        /**
         * 是否启用监控面板
         */
        @Builder.Default
        private boolean enabled = true;

        /**
         * 面板访问路径
         */
        @Builder.Default
        private String path = "/monitor/dashboard";

        /**
         * 面板配置
         */
        private PanelConfig panel;

        /**
         * 面板配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PanelConfig {
            /**
             * 刷新间隔（秒）
             */
            @Builder.Default
            private int refreshInterval = 30;

            /**
             * 图表主题：LIGHT、DARK
             */
            @Builder.Default
            private String theme = "LIGHT";

            /**
             * 显示的图表类型
             */
            @Builder.Default
            private List<String> chartTypes = List.of("line", "bar", "pie", "heatmap");
        }
    }
}