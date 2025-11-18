package com.group.gateway.core.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

import ch.qos.logback.core.util.OptionHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志配置类
 * 负责配置网关的日志输出格式、文件滚动、监控等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties
public class LogConfig {
    
    private final Environment environment;
    private final Map<String, Appender<ILoggingEvent>> customAppenders = new ConcurrentHashMap<>();
    
    public LogConfig(Environment environment) {
        this.environment = environment;
    }
    
    @PostConstruct
    public void init() {
        try {
            configureCustomLogging();
            log.info("日志配置初始化完成");
        } catch (Exception e) {
            log.error("日志配置初始化失败", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        customAppenders.values().forEach(Appender::stop);
        customAppenders.clear();
        log.info("日志配置清理完成");
    }
    
    /**
     * 配置自定义日志
     */
    private void configureCustomLogging() {
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        String logLevel = environment.getProperty("LOG_LEVEL", "INFO");
        String logPattern = environment.getProperty("logging.pattern.console", 
                "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n");
        
        // 配置根日志级别
        context.getLogger("ROOT").setLevel(Level.valueOf(logLevel));
        
        // 添加JSON格式的访问日志Appender
        addJsonAccessLogAppender(context, logPattern);
        
        // 添加性能监控日志Appender
        addPerformanceLogAppender(context, logPattern);
        
        // 添加安全日志Appender
        addSecurityLogAppender(context, logPattern);
    }
    
    /**
     * 添加JSON格式访问日志Appender
     */
    private void addJsonAccessLogAppender(LoggerContext context, String logPattern) {
        try {
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("JSON_ACCESS_LOG");
            appender.setContext(context);
            appender.setFile("logs/access-json.log");
            
            // 配置时间滚动策略
            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setParent(appender);
            rollingPolicy.setFileNamePattern("logs/archive/access-json-%d{yyyy-MM-dd}.%i.log");
            rollingPolicy.setMaxHistory(30);
            rollingPolicy.setTotalSizeCap("3GB");
            rollingPolicy.start();
            
            appender.setRollingPolicy(rollingPolicy);
            
            // JSON格式布局
            PatternLayout jsonLayout = new PatternLayout();
            jsonLayout.setContext(context);
            jsonLayout.setPattern("%m%n"); // 只使用消息，JSON格式由日志内容控制
            jsonLayout.setOutputPatternAsJSONHeaders(false);
            jsonLayout.start();
            
            appender.setLayout(jsonLayout);
            appender.start();
            
            // 获取访问日志Logger并添加Appender
            org.slf4j.Logger accessLogger = context.getLogger("ACCESS_JSON_LOG");
            if (accessLogger instanceof ch.qos.logback.classic.Logger accessLog) {
                accessLog.addAppender(appender);
                accessLog.setLevel(Level.INFO);
                accessLog.setAdditive(false);
            }
            
            customAppenders.put("JSON_ACCESS_LOG", appender);
            log.debug("JSON访问日志Appender配置完成");
            
        } catch (Exception e) {
            log.error("配置JSON访问日志Appender失败", e);
        }
    }
    
    /**
     * 添加性能监控日志Appender
     */
    private void addPerformanceLogAppender(LoggerContext context, String logPattern) {
        try {
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("PERFORMANCE_LOG");
            appender.setContext(context);
            appender.setFile("logs/performance.log");
            
            // 配置大小和时间滚动策略
            SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setParent(appender);
            rollingPolicy.setFileNamePattern("logs/archive/performance-%d{yyyy-MM-dd}.%i.log");
            rollingPolicy.setMaxFileSize("100MB");
            rollingPolicy.setMaxHistory(7);
            rollingPolicy.setTotalSizeCap("1GB");
            rollingPolicy.start();
            
            appender.setRollingPolicy(rollingPolicy);
            
            PatternLayout layout = new PatternLayout();
            layout.setContext(context);
            layout.setPattern(logPattern);
            layout.start();
            
            appender.setLayout(layout);
            appender.start();
            
            org.slf4j.Logger perfLogger = context.getLogger("PERFORMANCE_LOG");
            if (perfLogger instanceof ch.qos.logback.classic.Logger perfLog) {
                perfLog.addAppender(appender);
                perfLog.setLevel(Level.INFO);
                perfLog.setAdditive(false);
            }
            
            customAppenders.put("PERFORMANCE_LOG", appender);
            log.debug("性能监控日志Appender配置完成");
            
        } catch (Exception e) {
            log.error("配置性能监控日志Appender失败", e);
        }
    }
    
    /**
     * 添加安全日志Appender
     */
    private void addSecurityLogAppender(LoggerContext context, String logPattern) {
        try {
            RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
            appender.setName("SECURITY_LOG");
            appender.setContext(context);
            appender.setFile("logs/security.log");
            
            TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(context);
            rollingPolicy.setParent(appender);
            rollingPolicy.setFileNamePattern("logs/archive/security-%d{yyyy-MM-dd}.%i.log");
            rollingPolicy.setMaxHistory(90); // 安全日志保留90天
            rollingPolicy.setTotalSizeCap("5GB");
            rollingPolicy.start();
            
            appender.setRollingPolicy(rollingPolicy);
            
            PatternLayout layout = new PatternLayout();
            layout.setContext(context);
            layout.setPattern(logPattern);
            layout.start();
            
            appender.setLayout(layout);
            appender.start();
            
            org.slf4j.Logger securityLogger = context.getLogger("SECURITY_LOG");
            if (securityLogger instanceof ch.qos.logback.classic.Logger securityLog) {
                securityLog.addAppender(appender);
                securityLog.setLevel(Level.INFO);
                securityLog.setAdditive(false);
            }
            
            customAppenders.put("SECURITY_LOG", appender);
            log.debug("安全日志Appender配置完成");
            
        } catch (Exception e) {
            log.error("配置安全日志Appender失败", e);
        }
    }
    
    /**
     * 记录JSON格式访问日志
     */
    public void logAccessJson(String traceId, String clientIp, String method, String path, 
                             int statusCode, long responseTime, String userAgent, String userId) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            
            Map<String, Object> logData = Map.of(
                "timestamp", System.currentTimeMillis(),
                "traceId", traceId,
                "clientIp", clientIp,
                "method", method,
                "path", path,
                "statusCode", statusCode,
                "responseTime", responseTime,
                "userAgent", userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 100)) : "",
                "userId", userId,
                "level", statusCode >= 400 ? "WARN" : "INFO"
            );
            
            String jsonLog = mapper.writeValueAsString(logData);
            org.slf4j.LoggerFactory.getLogger("ACCESS_JSON_LOG").info(jsonLog);
            
        } catch (Exception e) {
            log.error("记录JSON访问日志异常", e);
        }
    }
    
    /**
     * 记录性能监控日志
     */
    public void logPerformance(String traceId, String operation, long duration, 
                              String status, String details) {
        try {
            org.slf4j.Logger perfLogger = org.slf4j.LoggerFactory.getLogger("PERFORMANCE_LOG");
            perfLogger.info("性能监控 - traceId: {}, 操作: {}, 耗时: {}ms, 状态: {}, 详情: {}", 
                          traceId, operation, duration, status, details);
        } catch (Exception e) {
            log.error("记录性能监控日志异常", e);
        }
    }
    
    /**
     * 记录安全日志
     */
    public void logSecurity(String event, String source, String target, String details, String severity) {
        try {
            org.slf4j.Logger securityLogger = org.slf4j.LoggerFactory.getLogger("SECURITY_LOG");
            securityLogger.warn("安全事件 - 事件: {}, 来源: {}, 目标: {}, 详情: {}, 级别: {}", 
                               event, source, target, details, severity);
        } catch (Exception e) {
            log.error("记录安全日志异常", e);
        }
    }
    
    /**
     * 获取日志统计信息
     */
    public Map<String, Object> getLogStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("customAppendersCount", customAppenders.size());
        stats.put("appendertype", customAppenders.keySet());
        return stats;
    }
}