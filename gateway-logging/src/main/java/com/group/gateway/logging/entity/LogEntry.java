package com.group.gateway.logging.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * 日志实体类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class LogEntry {
    
    /**
     * 日志ID
     */
    private String id;
    
    /**
     * 时间戳
     */
    private Instant timestamp;
    
    /**
     * 日志级别
     */
    private LogLevel level;
    
    /**
     * 日志记录器名称
     */
    private String loggerName;
    
    /**
     * 线程名称
     */
    private String threadName;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 异常信息
     */
    private ThrowableInfo throwable;
    
    /**
     * 标签
     */
    private Map<String, Object> tags;
    
    /**
     * 自定义字段
     */
    private Map<String, Object> fields;
    
    /**
     * HTTP请求信息
     */
    private HttpRequestInfo httpRequest;
    
    /**
     * 网关信息
     */
    private GatewayInfo gatewayInfo;
    
    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;
    
    /**
     * 跟踪ID
     */
    private String traceId;
    
    /**
     * 跨度ID
     */
    private String spanId;
    
    /**
     * 数据源
     */
    private String dataSource;
    
    /**
     * 索引名
     */
    private String indexName;
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        TRACE, DEBUG, INFO, WARN, ERROR, FATAL
    }
    
    /**
     * 异常信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThrowableInfo {
        
        /**
         * 异常类名
         */
        private String className;
        
        /**
         * 异常消息
         */
        private String message;
        
        /**
         * 堆栈跟踪
         */
        private List<String> stackTrace;
        
        /**
         * 原因异常
         */
        private ThrowableInfo cause;
    }
    
    /**
     * HTTP请求信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HttpRequestInfo {
        
        /**
         * 请求方法
         */
        private String method;
        
        /**
         * 请求URL
         */
        private String url;
        
        /**
         * 请求路径
         */
        private String path;
        
        /**
         * 请求参数
         */
        private Map<String, Object> parameters;
        
        /**
         * 请求头
         */
        private Map<String, String> headers;
        
        /**
         * 请求体
         */
        private String requestBody;
        
        /**
         * 响应状态码
         */
        private Integer statusCode;
        
        /**
         * 响应头
         */
        private Map<String, String> responseHeaders;
        
        /**
         * 响应体
         */
        private String responseBody;
        
        /**
         * 客户端IP
         */
        private String clientIp;
        
        /**
         * 用户代理
         */
        private String userAgent;
        
        /**
         * 会话ID
         */
        private String sessionId;
    }
    
    /**
     * 网关信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GatewayInfo {
        
        /**
         * 网关实例ID
         */
        private String instanceId;
        
        /**
         * 网关名称
         */
        private String name;
        
        /**
         * 网关版本
         */
        private String version;
        
        /**
         * 路由ID
         */
        private String routeId;
        
        /**
         * 目标服务
         */
        private String targetService;
        
        /**
         * 目标URL
         */
        private String targetUrl;
        
        /**
         * 过滤器信息
         */
        private List<String> filters;
        
        /**
         * 谓词信息
         */
        private List<String> predicates;
    }
    
    /**
     * 性能指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        
        /**
         * 总耗时（毫秒）
         */
        private Long totalTime;
        
        /**
         * 请求处理时间
         */
        private Long requestTime;
        
        /**
         * 响应处理时间
         */
        private Long responseTime;
        
        /**
         * 数据库查询时间
         */
        private Long dbTime;
        
        /**
         * 外部服务调用时间
         */
        private Long externalTime;
        
        /**
         * 缓存命中
         */
        private Boolean cacheHit;
        
        /**
         * 内存使用
         */
        private Long memoryUsage;
        
        /**
         * CPU使用率
         */
        private Double cpuUsage;
    }
    
    /**
     * 静态工厂方法
     */
    public static LogEntry createTrace(String loggerName, String message) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.TRACE)
                .loggerName(loggerName)
                .message(message)
                .build();
    }
    
    public static LogEntry createDebug(String loggerName, String message) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.DEBUG)
                .loggerName(loggerName)
                .message(message)
                .build();
    }
    
    public static LogEntry createInfo(String loggerName, String message) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.INFO)
                .loggerName(loggerName)
                .message(message)
                .build();
    }
    
    public static LogEntry createWarn(String loggerName, String message) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.WARN)
                .loggerName(loggerName)
                .message(message)
                .build();
    }
    
    public static LogEntry createError(String loggerName, String message, Throwable throwable) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.ERROR)
                .loggerName(loggerName)
                .message(message)
                .throwable(throwable != null ? ThrowableInfo.builder()
                        .className(throwable.getClass().getName())
                        .message(throwable.getMessage())
                        .stackTrace(StackTraceUtil.toStackTraceList(throwable))
                        .build() : null)
                .build();
    }
    
    public static LogEntry createFatal(String loggerName, String message, Throwable throwable) {
        return builder()
                .timestamp(Instant.now())
                .level(LogLevel.FATAL)
                .loggerName(loggerName)
                .message(message)
                .throwable(throwable != null ? ThrowableInfo.builder()
                        .className(throwable.getClass().getName())
                        .message(throwable.getMessage())
                        .stackTrace(StackTraceUtil.toStackTraceList(throwable))
                        .build() : null)
                .build();
    }
    
    /**
     * 堆栈跟踪工具类
     */
    public static class StackTraceUtil {
        
        public static List<String> toStackTraceList(Throwable throwable) {
            if (throwable == null) {
                return List.of();
            }
            
            return List.of(throwable.getStackTrace())
                    .stream()
                    .map(StackTraceElement::toString)
                    .toList();
        }
    }
}