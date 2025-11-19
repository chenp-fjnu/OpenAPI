package com.group.gateway.logging.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.group.gateway.logging.entity.LogEntry;
import com.group.gateway.logging.config.LoggingProperties;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 日志文件写入工具类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogFileWriter {
    
    private final LoggingProperties loggingProperties;
    
    // 文件写入相关
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private Path logDirectory;
    private String currentFileName;
    private PrintWriter currentWriter;
    private final Object writerLock = new Object();
    
    // 写入队列和线程池
    private final BlockingQueue<LogEntry> writeQueue = new LinkedBlockingQueue<>();
    private final ExecutorService writeExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    public LogFileWriter(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
        
        // 初始化线程池
        this.writeExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "log-file-writer-" + Thread.currentThread().getId());
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * 初始化文件写入器
     */
    public void initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            try {
                // 创建日志目录
                String path = loggingProperties.getStorage().getFile().getPath();
                logDirectory = Paths.get(path).toAbsolutePath().normalize();
                
                // 替换环境变量
                logDirectory = Paths.get(expandEnvironmentVariables(logDirectory.toString()));
                
                if (!Files.exists(logDirectory)) {
                    Files.createDirectories(logDirectory);
                    log.info("创建日志目录: {}", logDirectory);
                }
                
                // 启动写入线程
                startWriterThread();
                
                log.info("日志文件写入器初始化完成，目录: {}", logDirectory);
                
            } catch (Exception e) {
                log.error("初始化日志文件写入器失败", e);
                isInitialized.set(false);
            }
        }
    }
    
    /**
     * 写入日志条目
     */
    public void writeLogEntry(LogEntry logEntry) {
        if (!isInitialized.get()) {
            initialize();
        }
        
        try {
            if (!writeQueue.offer(logEntry, 100, TimeUnit.MILLISECONDS)) {
                log.warn("写入队列已满，跳过日志: {}", logEntry.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("写入日志时被中断", e);
        }
    }
    
    /**
     * 启动写入线程
     */
    private void startWriterThread() {
        if (isRunning.compareAndSet(false, true)) {
            writeExecutor.submit(this::writeLoop);
        }
    }
    
    /**
     * 写入循环
     */
    private void writeLoop() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                LogEntry logEntry = writeQueue.poll(1, TimeUnit.SECONDS);
                if (logEntry != null) {
                    writeToFile(logEntry);
                }
                
                // 检查是否需要切换文件
                checkFileRotation();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("文件写入循环异常", e);
                
                // 短暂暂停避免错误风暴
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 写入文件
     */
    private void writeToFile(LogEntry logEntry) {
        synchronized (writerLock) {
            try {
                // 确保文件已打开
                if (currentWriter == null || !isCurrentFileValid()) {
                    openNewFile();
                }
                
                // 写入日志
                String formattedLog = formatLogEntry(logEntry);
                currentWriter.println(formattedLog);
                currentWriter.flush();
                
            } catch (Exception e) {
                log.error("写入日志文件失败", e);
            }
        }
    }
    
    /**
     * 格式化日志条目
     */
    private String formatLogEntry(LogEntry logEntry) {
        StringBuilder sb = new StringBuilder();
        
        // 时间戳
        sb.append(logEntry.getTimestamp() != null ? 
                logEntry.getTimestamp().toString() : LocalDateTime.now().toString());
        sb.append(" ");
        
        // 日志级别
        sb.append(String.format("%-5s", logEntry.getLevel()));
        sb.append(" ");
        
        // 线程名称
        sb.append(String.format("[%-15s]", logEntry.getThreadName()));
        sb.append(" ");
        
        // 记录器名称
        sb.append(String.format("%-30s", logEntry.getLoggerName()));
        sb.append(" - ");
        
        // 消息内容
        sb.append(logEntry.getMessage());
        
        // 附加信息
        if (logEntry.getTraceId() != null) {
            sb.append(" [traceId=").append(logEntry.getTraceId()).append("]");
        }
        if (logEntry.getSpanId() != null) {
            sb.append(" [spanId=").append(logEntry.getSpanId()).append("]");
        }
        
        // 异常信息
        if (logEntry.getThrowable() != null) {
            sb.append("\n");
            sb.append(formatThrowable(logEntry.getThrowable()));
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化异常信息
     */
    private String formatThrowable(LogEntry.ThrowableInfo throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClassName());
        if (throwable.getMessage() != null) {
            sb.append(": ").append(throwable.getMessage());
        }
        sb.append("\n");
        
        if (throwable.getStackTrace() != null) {
            for (String stackTrace : throwable.getStackTrace()) {
                sb.append("\tat ").append(stackTrace).append("\n");
            }
        }
        
        // 递归处理原因异常
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(formatThrowable(throwable.getCause()));
        }
        
        return sb.toString();
    }
    
    /**
     * 打开新文件
     */
    private void openNewFile() {
        try {
            // 关闭旧文件
            closeCurrentFile();
            
            // 生成新文件名
            String fileName = generateFileName();
            Path filePath = logDirectory.resolve(fileName);
            
            // 创建文件写入器
            FileOutputStream fos = new FileOutputStream(filePath.toFile(), true);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
            currentWriter = new PrintWriter(osw, true); // auto-flush
            
            currentFileName = fileName;
            
            log.debug("打开日志文件: {}", filePath);
            
        } catch (Exception e) {
            log.error("打开日志文件失败", e);
        }
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName() {
        LoggingProperties.StorageConfig.FileStorage fileConfig = loggingProperties.getStorage().getFile();
        String pattern = fileConfig.getFileNamePattern();
        
        // 简单的日期格式化替换
        String fileName = pattern.replace("%d{yyyy-MM-dd}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        // 添加索引后缀
        fileName += ".0"; // TODO: 实现多文件轮转逻辑
        
        return fileName;
    }
    
    /**
     * 检查文件轮转
     */
    private void checkFileRotation() {
        // TODO: 实现文件轮转逻辑
        // 检查文件大小、时间等条件
    }
    
    /**
     * 检查当前文件是否有效
     */
    private boolean isCurrentFileValid() {
        if (currentFileName == null) {
            return false;
        }
        
        Path filePath = logDirectory.resolve(currentFileName);
        return Files.exists(filePath);
    }
    
    /**
     * 关闭当前文件
     */
    private void closeCurrentFile() {
        if (currentWriter != null) {
            try {
                currentWriter.close();
            } catch (Exception e) {
                log.warn("关闭日志文件失败", e);
            } finally {
                currentWriter = null;
                currentFileName = null;
            }
        }
    }
    
    /**
     * 扩展环境变量
     */
    private String expandEnvironmentVariables(String path) {
        // 简单的环境变量替换
        String expanded = path;
        if (expanded.contains("${user.home}")) {
            expanded = expanded.replace("${user.home}", System.getProperty("user.home"));
        }
        return expanded;
    }
    
    /**
     * 强制刷盘
     */
    public void flush() {
        synchronized (writerLock) {
            if (currentWriter != null) {
                currentWriter.flush();
            }
        }
    }
    
    /**
     * 关闭写入器
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭日志文件写入器");
        
        // 停止写入线程
        isRunning.set(false);
        
        // 关闭当前文件
        closeCurrentFile();
        
        // 关闭线程池
        writeExecutor.shutdown();
        try {
            if (!writeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                writeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writeExecutor.shutdownNow();
        }
    }
}