package com.group.gateway.ratelimit.strategy;

import org.springframework.web.server.ServerWebExchange;

/**
 * 限流策略接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
public interface RateLimitStrategy {

    /**
     * 检查是否超过限流阈值
     *
     * @param exchange 请求交换对象
     * @param key 限流键
     * @param limitParams 限流参数
     * @return 限流检查结果
     */
    RateLimitResult isAllowed(ServerWebExchange exchange, String key, LimitParams limitParams);

    /**
     * 获取策略名称
     *
     * @return 策略名称
     */
    String getStrategyName();

    /**
     * 获取支持的时间窗口类型
     *
     * @return 时间窗口类型
     */
    WindowType getWindowType();

    /**
     * 时间窗口类型枚举
     */
    enum WindowType {
        /**
         * 滑动窗口
         */
        SLIDING,
        
        /**
         * 固定窗口
         */
        FIXED,
        
        /**
         * 令牌桶（无窗口概念）
         */
        TOKEN_BUCKET
    }
}