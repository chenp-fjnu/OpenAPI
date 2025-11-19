package com.group.gateway.monitor.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 指标数据实体
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricData {
    
    /**
     * 指标名称
     */
    private String name;
    
    /**
     * 指标值
     */
    private double value;
    
    /**
     * 指标类型：COUNTER、GAGE、HISTOGRAM、TIMER
     */
    private MetricType type;
    
    /**
     * 指标标签
     */
    private Map<String, String> tags;
    
    /**
     * 指标维度
     */
    private Map<String, Object> dimensions;
    
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 数据源
     */
    private String source;
    
    /**
     * 指标类型枚举
     */
    public enum MetricType {
        /**
         * 计数器
         */
        COUNTER,
        
        /**
         * 计量器
         */
        GAGE,
        
        /**
         * 直方图
         */
        HISTOGRAM,
        
        /**
         * 计时器
         */
        TIMER
    }
    
    /**
     * 创建计数器指标数据
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @param timestamp 时间戳
     * @return 指标数据
     */
    public static MetricData counter(String name, double value, Map<String, String> tags, 
                                   Map<String, Object> dimensions, LocalDateTime timestamp) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .type(MetricType.COUNTER)
                .tags(tags)
                .dimensions(dimensions)
                .timestamp(timestamp)
                .build();
    }
    
    /**
     * 创建计量器指标数据
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @param timestamp 时间戳
     * @return 指标数据
     */
    public static MetricData gauge(String name, double value, Map<String, String> tags, 
                                 Map<String, Object> dimensions, LocalDateTime timestamp) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .type(MetricType.GAGE)
                .tags(tags)
                .dimensions(dimensions)
                .timestamp(timestamp)
                .build();
    }
    
    /**
     * 创建直方图指标数据
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @param timestamp 时间戳
     * @return 指标数据
     */
    public static MetricData histogram(String name, double value, Map<String, String> tags, 
                                     Map<String, Object> dimensions, LocalDateTime timestamp) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .type(MetricType.HISTOGRAM)
                .tags(tags)
                .dimensions(dimensions)
                .timestamp(timestamp)
                .build();
    }
    
    /**
     * 创建计时器指标数据
     *
     * @param name 指标名称
     * @param value 指标值（毫秒）
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @param timestamp 时间戳
     * @return 指标数据
     */
    public static MetricData timer(String name, double value, Map<String, String> tags, 
                                 Map<String, Object> dimensions, LocalDateTime timestamp) {
        return MetricData.builder()
                .name(name)
                .value(value)
                .type(MetricType.TIMER)
                .tags(tags)
                .dimensions(dimensions)
                .timestamp(timestamp)
                .build();
    }
}