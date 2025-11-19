package com.group.gateway.routing.service;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate Limit Counter
 * 限流计数器类
 * 用于跟踪请求频率，支持秒级、分钟级和小时级限流
 */
@Data
public class RateLimitCounter {

    // 时间窗口大小（毫秒）
    private static final long SECOND_WINDOW = 1000;
    private static final long MINUTE_WINDOW = 60 * 1000;
    private static final long HOUR_WINDOW = 60 * 60 * 1000;
    
    // 时间窗口索引大小
    private static final int SECOND_ARRAY_SIZE = 60; // 60秒
    private static final int MINUTE_ARRAY_SIZE = 60; // 60分钟
    private static final int HOUR_ARRAY_SIZE = 24;   // 24小时
    
    // 请求计数数组
    private final AtomicLongArray secondCounts = new AtomicLongArray(SECOND_ARRAY_SIZE);
    private final AtomicLongArray minuteCounts = new AtomicLongArray(MINUTE_ARRAY_SIZE);
    private final AtomicLongArray hourCounts = new AtomicLongArray(HOUR_ARRAY_SIZE);
    
    // 上次请求时间
    private volatile long lastRequestTime = 0L;
    
    // 总请求数
    private final AtomicLong totalRequests = new AtomicLong(0);
    
    /**
     * 记录请求
     */
    public void recordRequest(long currentTime) {
        // 更新时间窗口
        updateTimeWindows(currentTime);
        
        // 记录请求
        recordInWindow(secondCounts, currentTime, SECOND_WINDOW, SECOND_ARRAY_SIZE);
        recordInWindow(minuteCounts, currentTime, MINUTE_WINDOW, MINUTE_ARRAY_SIZE);
        recordInWindow(hourCounts, currentTime, HOUR_WINDOW, HOUR_ARRAY_SIZE);
        
        lastRequestTime = currentTime;
        totalRequests.incrementAndGet();
    }
    
    /**
     * 获取最近1秒内的请求数
     */
    public long getRequestsInLastSecond() {
        return getCountInWindow(secondCounts, SECOND_WINDOW, SECOND_ARRAY_SIZE);
    }
    
    /**
     * 获取最近1分钟内的请求数
     */
    public long getRequestsInLastMinute() {
        return getCountInWindow(minuteCounts, MINUTE_WINDOW, MINUTE_ARRAY_SIZE);
    }
    
    /**
     * 获取最近1小时内的请求数
     */
    public long getRequestsInLastHour() {
        return getCountInWindow(hourCounts, HOUR_WINDOW, HOUR_ARRAY_SIZE);
    }
    
    /**
     * 获取总请求数
     */
    public long getTotalRequests() {
        return totalRequests.get();
    }
    
    /**
     * 重置计数器
     */
    public void reset() {
        for (int i = 0; i < secondCounts.length(); i++) {
            secondCounts.set(i, 0);
        }
        for (int i = 0; i < minuteCounts.length(); i++) {
            minuteCounts.set(i, 0);
        }
        for (int i = 0; i < hourCounts.length(); i++) {
            hourCounts.set(i, 0);
        }
        totalRequests.set(0);
        lastRequestTime = 0;
    }
    
    /**
     * 更新时间窗口，清理过期的计数
     */
    private void updateTimeWindows(long currentTime) {
        // 清理过期的秒级计数
        clearExpiredCounts(secondCounts, currentTime, SECOND_WINDOW, SECOND_ARRAY_SIZE);
        
        // 清理过期的分钟级计数
        clearExpiredCounts(minuteCounts, currentTime, MINUTE_WINDOW, MINUTE_ARRAY_SIZE);
        
        // 清理过期的小时级计数
        clearExpiredCounts(hourCounts, currentTime, HOUR_WINDOW, HOUR_ARRAY_SIZE);
    }
    
    /**
     * 在指定时间窗口内记录请求
     */
    private void recordInWindow(AtomicLongArray counts, long currentTime, long windowSize, int arraySize) {
        long windowStart = currentTime - windowSize;
        int index = (int) ((currentTime / windowSize) % arraySize);
        int windowStartIndex = (int) ((windowStart / windowSize) % arraySize);
        
        // 如果时间窗口已滚动，重置对应索引的计数
        if (index != windowStartIndex) {
            counts.set(index, 0);
        }
        
        // 增加计数
        counts.incrementAndGet(index);
    }
    
    /**
     * 获取指定时间窗口内的总请求数
     */
    private long getCountInWindow(AtomicLongArray counts, long windowSize, int arraySize) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSize;
        
        long totalCount = 0;
        long currentWindow = currentTime / windowSize;
        long startWindow = windowStart / windowSize;
        
        // 遍历窗口范围内的所有索引
        for (long window = startWindow + 1; window <= currentWindow; window++) {
            int index = (int) (window % arraySize);
            totalCount += counts.get(index);
        }
        
        return totalCount;
    }
    
    /**
     * 清理过期的计数
     */
    private void clearExpiredCounts(AtomicLongArray counts, long currentTime, long windowSize, int arraySize) {
        long expiredWindowStart = currentTime - windowSize * arraySize;
        long currentWindow = currentTime / windowSize;
        
        // 如果时间窗口滚动超过数组大小，重置最早的计数
        for (int i = 0; i < arraySize; i++) {
            long window = currentWindow - i;
            if (window < expiredWindowStart / windowSize) {
                counts.set((int) (window % arraySize), 0);
            }
        }
    }
    
    /**
     * 获取计数器状态信息
     */
    public String getStatus() {
        return String.format("RateLimitCounter{total=%d, lastSecond=%d, lastMinute=%d, lastHour=%d, lastRequestTime=%d}",
            totalRequests.get(), getRequestsInLastSecond(), getRequestsInLastMinute(), 
            getRequestsInLastHour(), lastRequestTime);
    }
}