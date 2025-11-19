package com.group.gateway.logging.config;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * 日志模块配置属性
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "gateway.logging")
@Validated
public class LoggingProperties {
    
    /**
     * 是否启用日志模块
     */
    private boolean enabled = true;
    
    /**
     * 日志级别配置
     */
    private LevelConfig level = new LevelConfig();
    
    /**
     * 存储配置
     */
    private StorageConfig storage = new StorageConfig();
    
    /**
     * 输出配置
     */
    private OutputConfig output = new OutputConfig();
    
    /**
     * 采集配置
     */
    private CollectionConfig collection = new CollectionConfig();
    
    /**
     * 清洗配置
     */
    private CleaningConfig cleaning = new CleaningConfig();
    
    /**
     * 告警配置
     */
    private AlertConfig alert = new AlertConfig();
    
    /**
     * 性能配置
     */
    private PerformanceConfig performance = new PerformanceConfig();
    
    /**
     * 日志级别配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelConfig {
        
        /**
         * 根日志级别
         */
        @NotBlank(message = "根日志级别不能为空")
        private String root = "INFO";
        
        /**
         * 各模块日志级别
         */
        @Builder.Default
        private Map<String, String> modules = Map.of(
                "com.group.gateway", "INFO",
                "org.springframework.cloud.gateway", "DEBUG",
                "reactor", "WARN"
        );
        
        /**
         * HTTP请求日志级别
         */
        private String http = "INFO";
        
        /**
         * 安全相关日志级别
         */
        private String security = "WARN";
        
        /**
         * 性能日志级别
         */
        private String performance = "DEBUG";
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
         * 存储类型：FILE、DATABASE、ELK、KAFKA
         */
        @NotBlank(message = "存储类型不能为空")
        @Builder.Default
        private String type = "FILE";
        
        /**
         * 文件存储配置
         */
        private FileStorage file = new FileStorage();
        
        /**
         * 数据库存储配置
         */
        private DatabaseStorage database = new DatabaseStorage();
        
        /**
         * ELK存储配置
         */
        private ElkStorage elk = new ElkStorage();
        
        /**
         * Kafka存储配置
         */
        private KafkaStorage kafka = new KafkaStorage();
        
        /**
         * 文件存储配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileStorage {
            
            /**
             * 日志文件路径
             */
            @NotBlank(message = "日志文件路径不能为空")
            @Builder.Default
            private String path = "${user.home}/logs/gateway";
            
            /**
             * 日志文件名格式
             */
            @Builder.Default
            private String fileNamePattern = "gateway-%d{yyyy-MM-dd}.%i.log";
            
            /**
             * 单个日志文件最大大小
             */
            @Builder.Default
            private String maxFileSize = "100MB";
            
            /**
             * 最大保留天数
             */
            @Positive(message = "最大保留天数必须为正数")
            @Builder.Default
            private int maxHistory = 30;
            
            /**
             * 文件总数限制
             */
            @Positive(message = "文件总数限制必须为正数")
            @Builder.Default
            private int totalSizeCap = 10; // GB
        }
        
        /**
         * 数据库存储配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DatabaseStorage {
            
            /**
             * JDBC驱动类名
             */
            @Builder.Default
            private String driverClassName = "com.mysql.cj.jdbc.Driver";
            
            /**
             * JDBC连接URL
             */
            @Builder.Default
            private String url = "jdbc:mysql://localhost:3306/gateway_logs?useUnicode=true&characterEncoding=utf8&useSSL=false";
            
            /**
             * 数据库用户名
             */
            @Builder.Default
            private String username = "root";
            
            /**
             * 数据库密码
             */
            @Builder.Default
            private String password = "password";
            
            /**
             * 连接池大小
             */
            @Positive(message = "连接池大小必须为正数")
            @Builder.Default
            private int poolSize = 10;
            
            /**
             * 连接超时时间
             */
            @Builder.Default
            private Duration connectionTimeout = Duration.ofSeconds(30);
        }
        
        /**
         * ELK存储配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ElkStorage {
            
            /**
             * Elasticsearch节点地址
             */
            @NotEmpty(message = "Elasticsearch节点地址不能为空")
            @Builder.Default
            private List<String> nodes = List.of("http://localhost:9200");
            
            /**
             * 索引前缀
             */
            @Builder.Default
            private String indexPrefix = "gateway-logs";
            
            /**
             * 索引类型（ES7+固定为_doc）
             */
            @Builder.Default
            private String documentType = "_doc";
            
            /**
             * 批量提交大小
             */
            @Positive(message = "批量提交大小必须为正数")
            @Builder.Default
            private int bulkSize = 1000;
            
            /**
             * 刷新间隔（秒）
             */
            @Positive(message = "刷新间隔必须为正数")
            @Builder.Default
            private int refreshInterval = 30;
        }
        
        /**
         * Kafka存储配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class KafkaStorage {
            
            /**
             * Kafka代理地址
             */
            @NotEmpty(message = "Kafka代理地址不能为空")
            @Builder.Default
            private List<String> bootstrapServers = List.of("localhost:9092");
            
            /**
             * 主题名称
             */
            @NotBlank(message = "主题名称不能为空")
            @Builder.Default
            private String topic = "gateway-logs";
            
            /**
             * 消息键
             */
            @Builder.Default
            private String key = "gateway-log";
            
            /**
             * 生产者配置
             */
            @Builder.Default
            private Map<String, String> producerConfig = Map.of(
                    "acks", "all",
                    "retries", "3",
                    "batch.size", "16384",
                    "linger.ms", "5"
            );
        }
    }
    
    /**
     * 输出配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutputConfig {
        
        /**
         * 控制台输出配置
         */
        private ConsoleOutput console = new ConsoleOutput();
        
        /**
         * 文件输出配置
         */
        private FileOutput file = new FileOutput();
        
        /**
         * 控制台输出配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ConsoleOutput {
            
            /**
             * 是否启用控制台输出
             */
            private boolean enabled = true;
            
            /**
             * 控制台输出格式
             */
            @Builder.Default
            private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
        }
        
        /**
         * 文件输出配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class FileOutput {
            
            /**
             * 是否启用文件输出
             */
            private boolean enabled = true;
            
            /**
             * 是否启用Logstash编码
             */
            private boolean logstashEnabled = false;
            
            /**
             * 文件输出格式
             */
            @Builder.Default
            private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
            
            /**
             * Logstash配置
             */
            private LogstashConfig logstash = new LogstashConfig();
        }
        
        /**
         * Logstash配置
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class LogstashConfig {
            
            /**
             * Logstash服务器地址
             */
            @Builder.Default
            private String host = "localhost";
            
            /**
             * Logstash端口
             */
            @Positive(message = "Logstash端口必须为正数")
            @Builder.Default
            private int port = 5000;
            
            /**
             * 连接超时时间
             */
            @Builder.Default
            private Duration connectionTimeout = Duration.ofSeconds(5);
        }
    }
    
    /**
     * 采集配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectionConfig {
        
        /**
         * 是否启用日志采集
         */
        private boolean enabled = true;
        
        /**
         * 采集类型：FILE、STDIN、HTTP、KAFKA
         */
        @Builder.Default
        private String type = "FILE";
        
        /**
         * 采集间隔
         */
        @Builder.Default
        private Duration interval = Duration.ofSeconds(1);
        
        /**
         * 批处理大小
         */
        @Positive(message = "批处理大小必须为正数")
        @Builder.Default
        private int batchSize = 1000;
        
        /**
         * 采集过滤器
         */
        @Builder.Default
        private List<String> filters = List.of(
                "*.log",
                "error.log",
                "access.log"
        );
        
        /**
         * 排除规则
         */
        @Builder.Default
        private List<String> excludes = List.of(
                "*.tmp",
                "*.bak"
        );
    }
    
    /**
     * 清洗配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CleaningConfig {
        
        /**
         * 是否启用自动清洗
         */
        private boolean enabled = true;
        
        /**
         * 清洗时间（cron表达式）
         */
        @Builder.Default
        private String cron = "0 0 2 * * ?"; // 每天凌晨2点
        
        /**
         * 数据保留天数
         */
        @Positive(message = "数据保留天数必须为正数")
        @Builder.Default
        private int retentionDays = 30;
        
        /**
         * 最大磁盘使用率
         */
        @Min(value = 50, message = "最大磁盘使用率不能小于50%")
        @Max(value = 95, message = "最大磁盘使用率不能大于95%")
        @Builder.Default
        private int maxDiskUsage = 80; // %
    }
    
    /**
     * 告警配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertConfig {
        
        /**
         * 是否启用告警
         */
        private boolean enabled = false;
        
        /**
         * 告警规则
         */
        private List<AlertRule> rules = List.of(
                new AlertRule("ERROR", "ERROR日志数量超过阈值", "email", 100, Duration.ofMinutes(5)),
                new AlertRule("FATAL", "FATAL日志出现", "email", 1, Duration.ofMinutes(1))
        );
        
        /**
         * 告警规则
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AlertRule {
            
            /**
             * 告警级别
             */
            @NotBlank(message = "告警级别不能为空")
            private String level;
            
            /**
             * 告警描述
             */
            @NotBlank(message = "告警描述不能为空")
            private String description;
            
            /**
             * 告警渠道：email、webhook、sms
             */
            @NotBlank(message = "告警渠道不能为空")
            private String channel;
            
            /**
             * 告警阈值
             */
            @Positive(message = "告警阈值必须为正数")
            private int threshold;
            
            /**
             * 时间窗口
             */
            @NotNull(message = "时间窗口不能为空")
            private Duration window;
        }
    }
    
    /**
     * 性能配置
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceConfig {
        
        /**
         * 异步写入队列大小
         */
        @Positive(message = "异步写入队列大小必须为正数")
        @Builder.Default
        private int asyncQueueSize = 10000;
        
        /**
         * 线程池大小
         */
        @Positive(message = "线程池大小必须为正数")
        @Builder.Default
        private int threadPoolSize = 10;
        
        /**
         * 缓存大小
         */
        @Positive(message = "缓存大小必须为正数")
        @Builder.Default
        private int cacheSize = 1000;
        
        /**
         * 写入超时时间
         */
        @Builder.Default
        private Duration writeTimeout = Duration.ofSeconds(10);
    }
}