package com.group.gateway.logging.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import com.group.gateway.logging.service.LoggingService;
import com.group.gateway.logging.entity.LogEntry;
import com.group.gateway.logging.entity.LogIndex;
import com.group.gateway.logging.config.LoggingProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 日志控制器
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/logging")
@RequiredArgsConstructor
@Tag(name = "日志管理", description = "日志记录、查询、统计和管理相关接口")
@Validated
public class LoggingController {
    
    private final LoggingService loggingService;
    private final LoggingProperties loggingProperties;
    
    /**
     * 获取日志列表
     */
    @GetMapping("/logs")
    @Operation(summary = "获取日志列表", description = "根据条件分页查询日志记录")
    public ResponseEntity<List<LogEntry>> getLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String logger,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Positive int page,
            @RequestParam(defaultValue = "100") @Positive int size) {
        
        try {
            LoggingService.LogSearchCriteria criteria = LoggingService.LogSearchCriteria.builder()
                    .page(page)
                    .size(size)
                    .keyword(keyword)
                    .build();
            
            List<LogEntry> logs = loggingService.searchLogs(criteria);
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            log.error("获取日志列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取日志统计信息", description = "获取详细的日志统计和性能指标")
    public ResponseEntity<LoggingService.LogStatistics> getStatistics() {
        
        try {
            LoggingService.LogStatistics stats = loggingService.getStatistics();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("获取日志统计信息失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 记录日志
     */
    @PostMapping("/logs")
    @Operation(summary = "记录日志", description = "手动记录一条日志")
    public ResponseEntity<String> logMessage(@Valid @RequestBody LogRequest logRequest) {
        
        try {
            LogEntry logEntry = LogEntry.builder()
                    .level(LogEntry.LogLevel.valueOf(logRequest.getLevel().toUpperCase()))
                    .loggerName(logRequest.getLoggerName())
                    .message(logRequest.getMessage())
                    .tags(logRequest.getTags())
                    .fields(logRequest.getFields())
                    .build();
            
            loggingService.log(logEntry);
            
            return ResponseEntity.ok("日志记录成功");
            
        } catch (Exception e) {
            log.error("记录日志失败", e);
            return ResponseEntity.internalServerError().body("记录日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录TRACE级别日志
     */
    @PostMapping("/trace")
    @Operation(summary = "记录TRACE日志", description = "记录TRACE级别的日志")
    public ResponseEntity<String> trace(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.trace(loggerName, message);
            return ResponseEntity.ok("TRACE日志记录成功");
            
        } catch (Exception e) {
            log.error("记录TRACE日志失败", e);
            return ResponseEntity.internalServerError().body("记录TRACE日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录DEBUG级别日志
     */
    @PostMapping("/debug")
    @Operation(summary = "记录DEBUG日志", description = "记录DEBUG级别的日志")
    public ResponseEntity<String> debug(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.debug(loggerName, message);
            return ResponseEntity.ok("DEBUG日志记录成功");
            
        } catch (Exception e) {
            log.error("记录DEBUG日志失败", e);
            return ResponseEntity.internalServerError().body("记录DEBUG日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录INFO级别日志
     */
    @PostMapping("/info")
    @Operation(summary = "记录INFO日志", description = "记录INFO级别的日志")
    public ResponseEntity<String> info(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.info(loggerName, message);
            return ResponseEntity.ok("INFO日志记录成功");
            
        } catch (Exception e) {
            log.error("记录INFO日志失败", e);
            return ResponseEntity.internalServerError().body("记录INFO日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录WARN级别日志
     */
    @PostMapping("/warn")
    @Operation(summary = "记录WARN日志", description = "记录WARN级别的日志")
    public ResponseEntity<String> warn(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.warn(loggerName, message);
            return ResponseEntity.ok("WARN日志记录成功");
            
        } catch (Exception e) {
            log.error("记录WARN日志失败", e);
            return ResponseEntity.internalServerError().body("记录WARN日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录ERROR级别日志
     */
    @PostMapping("/error")
    @Operation(summary = "记录ERROR日志", description = "记录ERROR级别的日志")
    public ResponseEntity<String> error(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.error(loggerName, message, null);
            return ResponseEntity.ok("ERROR日志记录成功");
            
        } catch (Exception e) {
            log.error("记录ERROR日志失败", e);
            return ResponseEntity.internalServerError().body("记录ERROR日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 记录FATAL级别日志
     */
    @PostMapping("/fatal")
    @Operation(summary = "记录FATAL日志", description = "记录FATAL级别的日志")
    public ResponseEntity<String> fatal(
            @NotBlank @RequestParam String loggerName,
            @NotBlank @RequestParam String message) {
        
        try {
            loggingService.fatal(loggerName, message, null);
            return ResponseEntity.ok("FATAL日志记录成功");
            
        } catch (Exception e) {
            log.error("记录FATAL日志失败", e);
            return ResponseEntity.internalServerError().body("记录FATAL日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取日志索引列表
     */
    @GetMapping("/indexes")
    @Operation(summary = "获取日志索引列表", description = "获取所有日志索引信息")
    public ResponseEntity<List<LogIndex>> getIndexes() {
        
        try {
            List<LogIndex> indexes = loggingService.getIndexes();
            return ResponseEntity.ok(indexes);
            
        } catch (Exception e) {
            log.error("获取日志索引列表失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 创建日志索引
     */
    @PostMapping("/indexes")
    @Operation(summary = "创建日志索引", description = "创建一个新的日志索引")
    public ResponseEntity<LogIndex> createIndex(
            @NotBlank @RequestParam String indexName,
            @RequestParam(required = false) String description) {
        
        try {
            LogIndex index = loggingService.createIndex(indexName, description);
            return ResponseEntity.ok(index);
            
        } catch (Exception e) {
            log.error("创建日志索引失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 批量记录日志
     */
    @PostMapping("/batch")
    @Operation(summary = "批量记录日志", description = "批量记录多条日志")
    public ResponseEntity<String> batchLog(@Valid @RequestBody List<@NotNull LogRequest> logRequests) {
        
        try {
            List<LogEntry> logEntries = logRequests.stream()
                    .map(request -> LogEntry.builder()
                            .level(LogEntry.LogLevel.valueOf(request.getLevel().toUpperCase()))
                            .loggerName(request.getLoggerName())
                            .message(request.getMessage())
                            .tags(request.getTags())
                            .fields(request.getFields())
                            .build())
                    .toList();
            
            loggingService.batchLog(logEntries);
            
            return ResponseEntity.ok("批量日志记录成功，共记录 " + logEntries.size() + " 条日志");
            
        } catch (Exception e) {
            log.error("批量记录日志失败", e);
            return ResponseEntity.internalServerError().body("批量记录日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期日志
     */
    @PostMapping("/cleanup")
    @Operation(summary = "清理过期日志", description = "手动触发清理过期日志任务")
    public ResponseEntity<String> cleanup() {
        
        try {
            loggingService.cleanupExpiredLogs();
            return ResponseEntity.ok("清理过期日志任务已触发");
            
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            return ResponseEntity.internalServerError().body("清理过期日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取模块配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取模块配置", description = "获取日志模块的配置信息")
    public ResponseEntity<LoggingProperties> getConfig() {
        
        try {
            // 返回配置信息（注意敏感信息脱敏）
            LoggingProperties config = LoggingProperties.builder()
                    .enabled(loggingProperties.isEnabled())
                    .level(loggingProperties.getLevel())
                    .storage(LoggingProperties.StorageConfig.builder()
                            .type(loggingProperties.getStorage().getType())
                            .file(loggingProperties.getStorage().getFile())
                            .elk(loggingProperties.getStorage().getElk())
                            .kafka(loggingProperties.getStorage().getKafka())
                            .build())
                    .collection(loggingProperties.getCollection())
                    .cleaning(loggingProperties.getCleaning())
                    .build();
            
            return ResponseEntity.ok(config);
            
        } catch (Exception e) {
            log.error("获取模块配置失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取模块状态
     */
    @GetMapping("/status")
    @Operation(summary = "获取模块状态", description = "获取日志模块的运行状态")
    public ResponseEntity<Map<String, Object>> getStatus() {
        
        try {
            LoggingService.LogStatistics stats = loggingService.getStatistics();
            
            Map<String, Object> status = Map.of(
                    "enabled", loggingProperties.isEnabled(),
                    "uptime", "TODO", // TODO: 实现运行时间统计
                    "totalLogs", stats.getTotalLogs(),
                    "errorLogs", stats.getErrorLogs(),
                    "warnLogs", stats.getWarnLogs(),
                    "cacheSize", stats.getCacheSize(),
                    "queueSize", stats.getQueueSize(),
                    "indexCount", stats.getIndexCount(),
                    "timestamp", stats.getTimestamp()
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("获取模块状态失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查日志模块的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        
        try {
            LoggingService.LogStatistics stats = loggingService.getStatistics();
            
            // 检查基本状态
            boolean isHealthy = loggingProperties.isEnabled() && 
                              stats.getCacheSize() < Long.MAX_VALUE &&
                              !Thread.currentThread().isInterrupted();
            
            Map<String, Object> health = Map.of(
                    "status", isHealthy ? "UP" : "DOWN",
                    "enabled", loggingProperties.isEnabled(),
                    "timestamp", System.currentTimeMillis(),
                    "details", Map.of(
                            "totalLogs", stats.getTotalLogs(),
                            "queueSize", stats.getQueueSize(),
                            "cacheSize", stats.getCacheSize()
                    )
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseEntity.ok(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));
        }
    }
    
    /**
     * 日志记录请求
     */
    public record LogRequest(
            @NotBlank(message = "日志级别不能为空")
            @Size(min = 1, max = 10, message = "日志级别长度必须在1-10个字符之间")
            String level,
            
            @NotBlank(message = "记录器名称不能为空")
            @Size(min = 1, max = 100, message = "记录器名称长度必须在1-100个字符之间")
            String loggerName,
            
            @NotBlank(message = "消息内容不能为空")
            @Size(min = 1, max = 5000, message = "消息内容长度必须在1-5000个字符之间")
            String message,
            
            Map<String, Object> tags,
            
            Map<String, Object> fields
    ) {}
}