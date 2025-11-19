package com.group.gateway.ratelimit.strategy;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 限流参数
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LimitParams {
    
    /**
     * 请求速率（每秒请求数）
     */
    private double rate;
    
    /**
     * 突发容量（最大允许的突发请求数）
     */
    private long capacity;
    
    /**
     * 时间窗口大小（秒）
     */
    private int windowSize;
    
    /**
     * 算法类型
     */
    private AlgorithmType algorithmType;
    
    /**
     * 算法类型枚举
     */
    public enum AlgorithmType {
        /**
         * 令牌桶算法
         */
        TOKEN_BUCKET,
        
        /**
         * 滑动窗口算法
         */
        SLIDING_WINDOW,
        
        /**
         * 固定窗口算法
         */
        FIXED_WINDOW
    }
    
    /**
     * 创建令牌桶参数
     *
     * @param rate 速率
     * @param capacity 容量
     * @return 限流参数
     */
    public static LimitParams tokenBucket(double rate, long capacity) {
        return LimitParams.builder()
                .rate(rate)
                .capacity(capacity)
                .algorithmType(AlgorithmType.TOKEN_BUCKET)
                .build();
    }
    
    /**
     * 创建滑动窗口参数
     *
     * @param rate 速率
     * @param capacity 容量
     * @param windowSize 窗口大小
     * @return 限流参数
     */
    public static LimitParams slidingWindow(double rate, long capacity, int windowSize) {
        return LimitParams.builder()
                .rate(rate)
                .capacity(capacity)
                .windowSize(windowSize)
                .algorithmType(AlgorithmType.SLIDING_WINDOW)
                .build();
    }
    
    /**
     * 创建固定窗口参数
     *
     * @param rate 速率
     * @param capacity 容量
     * @param windowSize 窗口大小
     * @return 限流参数
     */
    public static LimitParams fixedWindow(double rate, long capacity, int windowSize) {
        return LimitParams.builder()
                .rate(rate)
                .capacity(capacity)
                .windowSize(windowSize)
                .algorithmType(AlgorithmType.FIXED_WINDOW)
                .build();
    }
}