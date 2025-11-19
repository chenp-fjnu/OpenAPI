package com.group.gateway.logging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;

import com.group.gateway.logging.entity.LogEntry;
import com.group.gateway.logging.entity.LogIndex;
import com.group.gateway.logging.config.LoggingProperties;

import jakarta.annotation.PreDestroy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.group.gateway.logging.entity.LogEntry.*;

/**
 * 日志服务
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoggingService {
    
    private final LoggingProperties loggingProperties;
    
    // 日志缓存
    private final Map<String, LogEntry> logCache = new ConcurrentHashMap<>();
    private final AtomicLong cacheSize = new AtomicLong(0);
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // 日志索引管理
    private final Map<String, LogIndex> logIndexes = new ConcurrentHashMap<>();
    private final AtomicLong totalLogs = new AtomicLong(0);
    private final AtomicLong errorLogs = new AtomicLong(0);
    private final AtomicLong warnLogs = new AtomicLong(0);
    
    // 异步写入队列
    private final BlockingQueue<LogEntry> writeQueue = new LinkedBlockingQueue<>();
    private final ExecutorService writeExecutor;
    private final ScheduledExecutorService scheduler;
    
    // 性能统计
    private final Map<String, AtomicLong> logCountByLevel = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> logCountByLogger = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimeStats = new ConcurrentHashMap<>();
    
    public LoggingService(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
        
        // 初始化线程池
        int threadPoolSize = loggingProperties.getPerformance().getThreadPoolSize();
        this.writeExecutor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "logging-write-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });
        
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "logging-scheduler-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });
        
        // 启动异步写入线程
        startAsyncWriter();
        
        // 启动性能统计清理
        startStatsCleanup();
        
        // 初始化统计计数器
        initializeStatsCounters();
        
        log.info("日志服务初始化完成，配置：{}", loggingProperties);
    }
    
    /**
     * 记录日志
     */
    public void log(@NotNull @Valid LogEntry logEntry) {
        if (!loggingProperties.isEnabled()) {
            return;
        }
        
        try {
            // 验证日志条目
            validateLogEntry(logEntry);
            
            // 设置默认值
            setDefaultValues(logEntry);
            
            // 更新统计信息
            updateStatistics(logEntry);
            
            // 添加到缓存
            addToCache(logEntry);
            
            // 添加到写入队列
            if (!writeQueue.offer(logEntry)) {
                log.warn("写入队列已满，跳过日志记录：{}", logEntry.getMessage());
            }
            
        } catch (Exception e) {
            log.error("记录日志时发生错误", e);
        }
    }
    
    /**
     * 记录TRACE级别日志
     */
    public void trace(String loggerName, String message) {
        log(createTrace(loggerName, message));
    }
    
    /**
     * 记录DEBUG级别日志
     */
    public void debug(String loggerName, String message) {
        log(createDebug(loggerName, message));
    }
    
    /**
     * 记录INFO级别日志
     */
    public void info(String loggerName, String message) {
        log(createInfo(loggerName, message));
    }
    
    /**
     * 记录WARN级别日志
     */
    public void warn(String loggerName, String message) {
        log(createWarn(loggerName, message));
    }
    
    /**
     * 记录ERROR级别日志
     */
    public void error(String loggerName, String message, Throwable throwable) {
        log(createError(loggerName, message, throwable));
    }
    
    /**
     * 记录FATAL级别日志
     */
    public void fatal(String loggerName, String message, Throwable throwable) {
        log(createFatal(loggerName, message, throwable));
    }
    
    /**
     * 批量记录日志
     */
    @Async
    public void batchLog(List<@NotNull @Valid LogEntry> logEntries) {
        if (!loggingProperties.isEnabled() || logEntries.isEmpty()) {
            return;
        }
        
        logEntries.forEach(this::log);
    }
    
    /**
     * 获取日志统计信息
     */
    public LogStatistics getStatistics() {
        return LogStatistics.builder()
                .totalLogs(totalLogs.get())
                .errorLogs(errorLogs.get())
                .warnLogs(warnLogs.get())
                .cacheSize(cacheSize.get())
                .queueSize(writeQueue.size())
                .logCountByLevel(new HashMap<>(logCountByLevel))
                .logCountByLogger(new HashMap<>(logCountByLogger))
                .responseTimeStats(new HashMap<>(responseTimeStats))
                .indexCount(logIndexes.size())
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 搜索日志
     */
    public List<LogEntry> searchLogs(LogSearchCriteria criteria) {
        // TODO: 实现日志搜索逻辑
        return List.of();
    }
    
    /**
     * 获取日志索引
     */
    public List<LogIndex> getIndexes() {
        return new ArrayList<>(logIndexes.values());
    }
    
    /**
     * 创建索引
     */
    public LogIndex createIndex(String indexName, String description) {
        LogIndex index = LogIndex.createIndex(indexName, description);
        logIndexes.put(indexName, index);
        return index;
    }
    
    /**
     * 清理过期日志
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点
    public void cleanupExpiredLogs() {
        if (!loggingProperties.getCleaning().isEnabled()) {
            return;
        }
        
        try {
            int retentionDays = loggingProperties.getCleaning().getRetentionDays();
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            log.info("开始清理过期日志，清理时间：{}，保留天数：{}", cutoffTime, retentionDays);
            
            // TODO: 实现清理逻辑
            
        } catch (Exception e) {
            log.error("清理过期日志时发生错误", e);
        }
    }
    
    /**
     * 检查磁盘使用率
     */
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void checkDiskUsage() {
        // TODO: 实现磁盘使用率检查逻辑
    }
    
    /**
     * 启动异步写入线程
     */
    private void startAsyncWriter() {
        writeExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LogEntry logEntry = writeQueue.take();
                    writeLogEntry(logEntry);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("异步写入日志时发生错误", e);
                }
            }
        });
    }
    
    /**
     * 启动性能统计清理
     */
    private void startStatsCleanup() {
        scheduler.scheduleAtFixedRate(this::cleanupOldStats, 3600, 3600, TimeUnit.SECONDS);
    }
    
    /**
     * 初始化统计计数器
     */
    private void initializeStatsCounters() {
        for (LogLevel level : LogLevel.values()) {
            logCountByLevel.put(level.name(), new AtomicLong(0));
        }
    }
    
    /**
     * 写入日志条目
     */
    private void writeLogEntry(LogEntry logEntry) {
        try {
            String storageType = loggingProperties.getStorage().getType();
            
            switch (storageType.toUpperCase()) {
                case "FILE":
                    writeToFile(logEntry);
                    break;
                case "DATABASE":
                    writeToDatabase(logEntry);
                    break;
                case "ELK":
                    writeToElk(logEntry);
                    break;
                case "KAFKA":
                    writeToKafka(logEntry);
                    break;
                default:
                    log.warn("未知的存储类型：{}，使用默认文件存储", storageType);
                    writeToFile(logEntry);
            }
            
        } catch (Exception e) {
            log.error("写入日志时发生错误", e);
        }
    }
    
    /**
     * 写入文件
     */
    private void writeToFile(LogEntry logEntry) {
        // TODO: 实现文件写入逻辑
        log.debug("写入文件：{}", logEntry.getMessage());
    }
    
    /**
     * 写入数据库
     */
    private void writeToDatabase(LogEntry logEntry) {
        // TODO: 实现数据库写入逻辑
        log.debug("写入数据库：{}", logEntry.getMessage());
    }
    
    /**
     * 写入ELK
     */
    private void writeToElk(LogEntry logEntry) {
        // TODO: 实现ELK写入逻辑
        log.debug("写入ELK：{}", logEntry.getMessage());
    }
    
    /**
     * 写入Kafka
     */
    private void writeToKafka(LogEntry logEntry) {
        // TODO: 实现Kafka写入逻辑
        log.debug("写入Kafka：{}", logEntry.getMessage());
    }
    
    /**
     * 验证日志条目
     */
    private void validateLogEntry(LogEntry logEntry) {
        if (logEntry.getId() == null || logEntry.getId().trim().isEmpty()) {
            logEntry.setId(UUID.randomUUID().toString());
        }
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(Instant.now());
        }
        if (logEntry.getLoggerName() == null || logEntry.getLoggerName().trim().isEmpty()) {
            logEntry.setLoggerName("default");
        }
    }
    
    /**
     * 设置默认值
     */
    private void setDefaultValues(LogEntry logEntry) {
        if (logEntry.getThreadName() == null) {
            logEntry.setThreadName(Thread.currentThread().getName());
        }
        if (logEntry.getDataSource() == null) {
            logEntry.setDataSource("gateway-logging");
        }
        if (logEntry.getIndexName() == null) {
            logEntry.setIndexName("gateway-logs-" + 
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")));
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics(LogEntry logEntry) {
        totalLogs.incrementAndGet();
        
        // 按级别统计
        String level = logEntry.getLevel().name();
        logCountByLevel.computeIfAbsent(level, k -> new AtomicLong(0)).incrementAndGet();
        
        // 按记录器统计
        String loggerName = logEntry.getLoggerName();
        logCountByLogger.computeIfAbsent(loggerName, k -> new AtomicLong(0)).incrementAndGet();
        
        // 错误和警告计数
        if (logEntry.getLevel() == LogLevel.ERROR) {
            errorLogs.incrementAndGet();
        } else if (logEntry.getLevel() == LogLevel.WARN) {
            warnLogs.incrementAndGet();
        }
        
        // 响应时间统计
        if (logEntry.getPerformanceMetrics() != null && 
            logEntry.getPerformanceMetrics().getTotalTime() != null) {
            String key = logEntry.getLoggerName();
            responseTimeStats.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                    .add(logEntry.getPerformanceMetrics().getTotalTime());
        }
    }
    
    /**
     * 添加到缓存
     */
    private void addToCache(LogEntry logEntry) {
        cacheLock.writeLock().lock();
        try {
            int cacheLimit = loggingProperties.getPerformance().getCacheSize();
            
            if (cacheSize.get() >= cacheLimit) {
                // 移除最旧的条目
                String oldestKey = logCache.keySet().iterator().next();
                logCache.remove(oldestKey);
                cacheSize.decrementAndGet();
            }
            
            logCache.put(logEntry.getId(), logEntry);
            cacheSize.incrementAndGet();
            
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 清理旧的统计信息
     */
    private void cleanupOldStats() {
        responseTimeStats.forEach((key, values) -> {
            if (values.size() > 1000) {
                values.subList(0, values.size() - 1000).clear();
            }
        });
    }
    
    /**
     * 关闭服务
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭日志服务");
        
        // 关闭线程池
        writeExecutor.shutdown();
        scheduler.shutdown();
        
        try {
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
            
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 日志统计信息
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LogStatistics {
        
        private long totalLogs;
        private long errorLogs;
        private long warnLogs;
        private long cacheSize;
        private long queueSize;
        private Map<String, AtomicLong> logCountByLevel;
        private Map<String, AtomicLong> logCountByLogger;
        private Map<String, List<Long>> responseTimeStats;
        private int indexCount;
        private LocalDateTime timestamp;
    }
    
    /**
     * 日志搜索条件
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LogSearchCriteria {
        
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Set<LogLevel> levels;
        private Set<String> loggerNames;
        private String keyword;
        private Map<String, Object> tags;
        private int page = 1;
        private int size = 100;
    }
}