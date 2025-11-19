package com.group.gateway.monitor.controller;

import com.group.gateway.monitor.entity.MetricData;
import com.group.gateway.monitor.service.MetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 监控数据API控制器
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
@Tag(name = "监控数据管理", description = "提供指标数据收集、查询和管理功能")
public class MonitorController {
    
    private final MetricService metricService;
    
    /**
     * 获取所有指标数据
     *
     * @return 指标数据列表
     */
    @GetMapping("/metrics")
    @Operation(summary = "获取所有指标数据", description = "返回系统中所有收集的指标数据")
    public ResponseEntity<List<MetricData>> getAllMetrics() {
        log.info("获取所有指标数据请求");
        try {
            List<MetricData> metrics = metricService.getAllMetrics();
            log.info("成功获取 {} 个指标数据", metrics.size());
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("获取指标数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指标统计信息
     *
     * @param name 指标名称
     * @param tags 指标标签（可选）
     * @return 统计信息
     */
    @GetMapping("/metrics/{name}/statistics")
    @Operation(summary = "获取指标统计信息", description = "返回指定指标的详细统计信息")
    public ResponseEntity<MetricService.MetricStatistics> getMetricStatistics(
            @PathVariable String name,
            @RequestParam(required = false) Map<String, String> tags) {
        log.info("获取指标统计信息请求: name={}, tags={}", name, tags);
        try {
            Map<String, String> tagMap = tags != null ? tags : new HashMap<>();
            MetricService.MetricStatistics statistics = metricService.getMetricStatistics(name, tagMap);
            log.info("成功获取指标统计信息: {}", name);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取指标统计信息失败: name={}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 记录计数器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 记录的指标数据
     */
    @PostMapping("/metrics/counter/{name}")
    @Operation(summary = "记录计数器指标", description = "记录一个新的计数器指标数据")
    public ResponseEntity<MetricData> recordCounter(
            @PathVariable String name,
            @RequestParam(defaultValue = "1") double value,
            @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录计数器指标请求: name={}, value={}, tags={}", name, value, tags);
        try {
            Map<String, String> tagMap = tags != null ? tags : new HashMap<>();
            MetricData metricData = metricService.recordCounter(name, value, tagMap);
            log.info("成功记录计数器指标: {}", name);
            return ResponseEntity.ok(metricData);
        } catch (Exception e) {
            log.error("记录计数器指标失败: name={}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 记录计量器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 记录的指标数据
     */
    @PostMapping("/metrics/gauge/{name}")
    @Operation(summary = "记录计量器指标", description = "记录一个新的计量器指标数据")
    public ResponseEntity<MetricData> recordGauge(
            @PathVariable String name,
            @RequestParam double value,
            @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录计量器指标请求: name={}, value={}, tags={}", name, value, tags);
        try {
            Map<String, String> tagMap = tags != null ? tags : new HashMap<>();
            MetricData metricData = metricService.recordGauge(name, value, tagMap);
            log.info("成功记录计量器指标: {}", name);
            return ResponseEntity.ok(metricData);
        } catch (Exception e) {
            log.error("记录计量器指标失败: name={}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 记录直方图指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 记录的指标数据
     */
    @PostMapping("/metrics/histogram/{name}")
    @Operation(summary = "记录直方图指标", description = "记录一个新的直方图指标数据")
    public ResponseEntity<MetricData> recordHistogram(
            @PathVariable String name,
            @RequestParam double value,
            @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录直方图指标请求: name={}, value={}, tags={}", name, value, tags);
        try {
            Map<String, String> tagMap = tags != null ? tags : new HashMap<>();
            MetricData metricData = metricService.recordHistogram(name, value, tagMap);
            log.info("成功记录直方图指标: {}", name);
            return ResponseEntity.ok(metricData);
        } catch (Exception e) {
            log.error("记录直方图指标失败: name={}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 记录计时器指标
     *
     * @param name 指标名称
     * @param value 指标值（毫秒）
     * @param tags 指标标签
     * @return 记录的指标数据
     */
    @PostMapping("/metrics/timer/{name}")
    @Operation(summary = "记录计时器指标", description = "记录一个新的计时器指标数据")
    public ResponseEntity<MetricData> recordTimer(
            @PathVariable String name,
            @RequestParam double value,
            @RequestParam(required = false) Map<String, String> tags) {
        log.info("记录计时器指标请求: name={}, value={}, tags={}", name, value, tags);
        try {
            Map<String, String> tagMap = tags != null ? tags : new HashMap<>();
            MetricData metricData = metricService.recordTimer(name, value, tagMap);
            log.info("成功记录计时器指标: {}", name);
            return ResponseEntity.ok(metricData);
        } catch (Exception e) {
            log.error("记录计时器指标失败: name={}", name, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清理过期指标数据
     *
     * @param olderThanMinutes 过期时间（分钟）
     * @return 操作结果
     */
    @DeleteMapping("/metrics/cleanup")
    @Operation(summary = "清理过期指标", description = "清理指定时间之前的指标数据")
    public ResponseEntity<Map<String, Object>> cleanupExpiredMetrics(
            @RequestParam(defaultValue = "60") int olderThanMinutes) {
        log.info("清理过期指标数据请求: olderThanMinutes={}", olderThanMinutes);
        try {
            metricService.cleanExpiredMetrics(olderThanMinutes);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", String.format("成功清理 %d 分钟前的指标数据", olderThanMinutes));
            result.put("olderThanMinutes", olderThanMinutes);
            result.put("timestamp", LocalDateTime.now());
            
            log.info("成功清理过期指标数据");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("清理过期指标数据失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "清理过期指标数据失败: " + e.getMessage());
            result.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 获取监控健康状态
     *
     * @return 健康状态信息
     */
    @GetMapping("/health")
    @Operation(summary = "获取监控健康状态", description = "返回监控模块的健康状态和基本统计信息")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        log.info("获取监控健康状态请求");
        try {
            List<MetricData> allMetrics = metricService.getAllMetrics();
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("status", "UP");
            healthStatus.put("timestamp", LocalDateTime.now());
            healthStatus.put("totalMetrics", allMetrics.size());
            healthStatus.put("uptime", System.currentTimeMillis() - getStartTime());
            
            // 统计不同类型的指标数量
            int counterCount = 0;
            int gaugeCount = 0;
            int histogramCount = 0;
            int timerCount = 0;
            
            for (MetricData metric : allMetrics) {
                switch (metric.getType()) {
                    case COUNTER: counterCount++; break;
                    case GAGE: gaugeCount++; break;
                    case HISTOGRAM: histogramCount++; break;
                    case TIMER: timerCount++; break;
                }
            }
            
            healthStatus.put("metricTypes", Map.of(
                    "counter", counterCount,
                    "gauge", gaugeCount,
                    "histogram", histogramCount,
                    "timer", timerCount
            ));
            
            log.info("成功获取监控健康状态");
            return ResponseEntity.ok(healthStatus);
        } catch (Exception e) {
            log.error("获取监控健康状态失败", e);
            
            Map<String, Object> healthStatus = new HashMap<>();
            healthStatus.put("status", "DOWN");
            healthStatus.put("timestamp", LocalDateTime.now());
            healthStatus.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(healthStatus);
        }
    }
    
    /**
     * 获取启动时间（简化实现）
     */
    private long getStartTime() {
        // 实际应用中应该从应用启动时间获取
        return System.currentTimeMillis() - 60000; // 假设运行了1分钟
    }
}