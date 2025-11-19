package com.group.gateway.monitor.service;

import com.group.gateway.monitor.entity.MetricData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 核心监控服务
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class MetricService {
    
    /**
     * 指标存储（内存缓存）
     */
    private final Map<String, MetricCollector> collectors = new ConcurrentHashMap<>();
    
    /**
     * 计数器缓存
     */
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    
    /**
     * 计量器缓存
     */
    private final Map<String, Double> gauges = new ConcurrentHashMap<>();
    
    /**
     * 直方图数据缓存
     */
    private final Map<String, List<Double>> histograms = new ConcurrentHashMap<>();
    
    /**
     * 计时器数据缓存
     */
    private final Map<String, DoubleAdder> timers = new ConcurrentHashMap<>();
    
    /**
     * 指标收集器内部类
     */
    public static class MetricCollector {
        private final String name;
        private final MetricData.MetricType type;
        private volatile LocalDateTime lastUpdateTime;
        
        public MetricCollector(String name, MetricData.MetricType type) {
            this.name = name;
            this.type = type;
            this.lastUpdateTime = LocalDateTime.now();
        }
        
        public void updateTime() {
            this.lastUpdateTime = LocalDateTime.now();
        }
        
        public String getName() {
            return name;
        }
        
        public MetricData.MetricType getType() {
            return type;
        }
        
        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
    
    /**
     * 记录计数器指标
     *
     * @param name 指标名称
     * @param tags 指标标签
     * @return 指标数据
     */
    public MetricData recordCounter(String name, Map<String, String> tags) {
        return recordCounter(name, 1, tags);
    }
    
    /**
     * 记录计数器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 指标数据
     */
    public MetricData recordCounter(String name, double value, Map<String, String> tags) {
        return recordCounter(name, value, tags, Collections.emptyMap());
    }
    
    /**
     * 记录计数器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @return 指标数据
     */
    public MetricData recordCounter(String name, double value, Map<String, String> tags, 
                                  Map<String, Object> dimensions) {
        log.debug("记录计数器指标: {}, value: {}, tags: {}", name, value, tags);
        
        // 更新计数器
        String counterKey = buildKey(name, tags);
        counters.computeIfAbsent(counterKey, k -> new AtomicLong(0)).addAndGet((long) value);
        
        // 记录收集器
        collectors.computeIfAbsent(counterKey, k -> new MetricCollector(counterKey, MetricData.MetricType.COUNTER)).updateTime();
        
        // 创建指标数据
        return MetricData.counter(name, value, tags, dimensions, LocalDateTime.now());
    }
    
    /**
     * 记录计量器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 指标数据
     */
    public MetricData recordGauge(String name, double value, Map<String, String> tags) {
        return recordGauge(name, value, tags, Collections.emptyMap());
    }
    
    /**
     * 记录计量器指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @return 指标数据
     */
    public MetricData recordGauge(String name, double value, Map<String, String> tags, 
                                Map<String, Object> dimensions) {
        log.debug("记录计量器指标: {}, value: {}, tags: {}", name, value, tags);
        
        // 更新计量器
        String gaugeKey = buildKey(name, tags);
        gauges.put(gaugeKey, value);
        
        // 记录收集器
        collectors.computeIfAbsent(gaugeKey, k -> new MetricCollector(gaugeKey, MetricData.MetricType.GAGE)).updateTime();
        
        // 创建指标数据
        return MetricData.gauge(name, value, tags, dimensions, LocalDateTime.now());
    }
    
    /**
     * 记录直方图指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @return 指标数据
     */
    public MetricData recordHistogram(String name, double value, Map<String, String> tags) {
        return recordHistogram(name, value, tags, Collections.emptyMap());
    }
    
    /**
     * 记录直方图指标
     *
     * @param name 指标名称
     * @param value 指标值
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @return 指标数据
     */
    public MetricData recordHistogram(String name, double value, Map<String, String> tags, 
                                    Map<String, Object> dimensions) {
        log.debug("记录直方图指标: {}, value: {}, tags: {}", name, value, tags);
        
        // 更新直方图
        String histogramKey = buildKey(name, tags);
        histograms.computeIfAbsent(histogramKey, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(value);
        
        // 记录收集器
        collectors.computeIfAbsent(histogramKey, k -> new MetricCollector(histogramKey, MetricData.MetricType.HISTOGRAM)).updateTime();
        
        // 创建指标数据
        return MetricData.histogram(name, value, tags, dimensions, LocalDateTime.now());
    }
    
    /**
     * 记录计时器指标
     *
     * @param name 指标名称
     * @param value 指标值（毫秒）
     * @param tags 指标标签
     * @return 指标数据
     */
    public MetricData recordTimer(String name, double value, Map<String, String> tags) {
        return recordTimer(name, value, tags, Collections.emptyMap());
    }
    
    /**
     * 记录计时器指标
     *
     * @param name 指标名称
     * @param value 指标值（毫秒）
     * @param tags 指标标签
     * @param dimensions 指标维度
     * @return 指标数据
     */
    public MetricData recordTimer(String name, double value, Map<String, String> tags, 
                                Map<String, Object> dimensions) {
        log.debug("记录计时器指标: {}, value: {}, tags: {}", name, value, tags);
        
        // 更新计时器
        String timerKey = buildKey(name, tags);
        timers.computeIfAbsent(timerKey, k -> new DoubleAdder()).add(value);
        
        // 记录收集器
        collectors.computeIfAbsent(timerKey, k -> new MetricCollector(timerKey, MetricData.MetricType.TIMER)).updateTime();
        
        // 创建指标数据
        return MetricData.timer(name, value, tags, dimensions, LocalDateTime.now());
    }
    
    /**
     * 获取所有指标
     *
     * @return 指标数据列表
     */
    public List<MetricData> getAllMetrics() {
        List<MetricData> metrics = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // 获取计数器
        counters.forEach((key, value) -> {
            if (collectors.containsKey(key)) {
                MetricData.MetricType type = collectors.get(key).getType();
                if (type == MetricData.MetricType.COUNTER) {
                    String name = key.split(":")[0];
                    Map<String, String> tags = parseTags(key);
                    metrics.add(MetricData.counter(name, value.get(), tags, Collections.emptyMap(), now));
                }
            }
        });
        
        // 获取计量器
        gauges.forEach((key, value) -> {
            if (collectors.containsKey(key)) {
                MetricData.MetricType type = collectors.get(key).getType();
                if (type == MetricData.MetricType.GAGE) {
                    String name = key.split(":")[0];
                    Map<String, String> tags = parseTags(key);
                    metrics.add(MetricData.gauge(name, value, tags, Collections.emptyMap(), now));
                }
            }
        });
        
        return metrics;
    }
    
    /**
     * 获取指定指标的统计信息
     *
     * @param name 指标名称
     * @param tags 指标标签
     * @return 统计信息
     */
    public MetricStatistics getMetricStatistics(String name, Map<String, String> tags) {
        String key = buildKey(name, tags);
        MetricCollector collector = collectors.get(key);
        
        if (collector == null) {
            return new MetricStatistics(name, tags, 0, 0, 0, 0, 0, collector != null ? collector.getLastUpdateTime() : null);
        }
        
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int count = 0;
        
        // 根据指标类型获取数据
        switch (collector.getType()) {
            case COUNTER:
                AtomicLong counter = counters.get(key);
                if (counter != null) {
                    sum = counter.get();
                    count = 1;
                    min = counter.get();
                    max = counter.get();
                }
                break;
                
            case GAGE:
                Double gauge = gauges.get(key);
                if (gauge != null) {
                    sum = gauge;
                    count = 1;
                    min = gauge;
                    max = gauge;
                }
                break;
                
            case HISTOGRAM:
                List<Double> histogramData = histograms.get(key);
                if (histogramData != null && !histogramData.isEmpty()) {
                    synchronized (histogramData) {
                        for (Double value : histogramData) {
                            sum += value;
                            count++;
                            min = Math.min(min, value);
                            max = Math.max(max, value);
                        }
                    }
                }
                break;
                
            case TIMER:
                DoubleAdder timer = timers.get(key);
                if (timer != null) {
                    sum = timer.sum();
                    count = 1;
                    min = sum;
                    max = sum;
                }
                break;
        }
        
        double avg = count > 0 ? sum / count : 0;
        
        return new MetricStatistics(name, tags, count, sum, avg, min, max, collector.getLastUpdateTime());
    }
    
    /**
     * 清理过期的指标数据
     *
     * @param olderThanMinutes 过期时间（分钟）
     */
    public void cleanExpiredMetrics(int olderThanMinutes) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(olderThanMinutes);
        
        collectors.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            MetricCollector collector = entry.getValue();
            
            if (collector.getLastUpdateTime().isBefore(cutoffTime)) {
                log.debug("清理过期指标: {}", key);
                
                // 清理相关缓存数据
                counters.remove(key);
                gauges.remove(key);
                histograms.remove(key);
                timers.remove(key);
                
                return true;
            }
            
            return false;
        });
    }
    
    /**
     * 构建指标键
     *
     * @param name 指标名称
     * @param tags 指标标签
     * @return 指标键
     */
    private String buildKey(String name, Map<String, String> tags) {
        if (tags.isEmpty()) {
            return name;
        }
        
        StringBuilder sb = new StringBuilder(name);
        tags.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                sb.append(":").append(entry.getKey()).append("=").append(entry.getValue());
            });
        
        return sb.toString();
    }
    
    /**
     * 解析指标标签
     *
     * @param key 指标键
     * @return 指标标签
     */
    private Map<String, String> parseTags(String key) {
        Map<String, String> tags = new HashMap<>();
        
        int nameEnd = key.indexOf(':');
        if (nameEnd == -1) {
            return tags;
        }
        
        String tagsPart = key.substring(nameEnd + 1);
        String[] tagPairs = tagsPart.split(":");
        
        for (String pair : tagPairs) {
            String[] tag = pair.split("=", 2);
            if (tag.length == 2) {
                tags.put(tag[0], tag[1]);
            }
        }
        
        return tags;
    }
    
    /**
     * 指标统计信息内部类
     */
    public static class MetricStatistics {
        private final String name;
        private final Map<String, String> tags;
        private final int count;
        private final double sum;
        private final double avg;
        private final double min;
        private final double max;
        private final LocalDateTime lastUpdateTime;
        
        public MetricStatistics(String name, Map<String, String> tags, int count, double sum, 
                              double avg, double min, double max, LocalDateTime lastUpdateTime) {
            this.name = name;
            this.tags = tags != null ? Collections.unmodifiableMap(tags) : Collections.emptyMap();
            this.count = count;
            this.sum = sum;
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        // Getters
        public String getName() { return name; }
        public Map<String, String> getTags() { return tags; }
        public int getCount() { return count; }
        public double getSum() { return sum; }
        public double getAvg() { return avg; }
        public double getMin() { return min; }
        public double getMax() { return max; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
    }
}