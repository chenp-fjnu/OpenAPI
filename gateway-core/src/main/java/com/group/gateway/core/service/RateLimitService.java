package com.group.gateway.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 * 负责实现多种限流算法，包括令牌桶限流、滑动窗口限流等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    @Value("${gateway.rate-limit.redis.prefix:rate_limit:}")
    private String redisPrefix;
    
    @Value("${gateway.rate-limit.default.rps:100}")
    private long defaultRps;
    
    @Value("${gateway.rate-limit.default.tps:1000}")
    private long defaultTps;
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 检查IP限流
     */
    public Mono<RateLimitResult> checkIpRateLimit(ServerWebExchange exchange, long limit) {
        return Mono.fromCallable(() -> {
            String clientIp = extractClientIp(exchange);
            String key = redisPrefix + "ip:" + clientIp;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            long count = currentCount != null ? Long.parseLong(currentCount) : 0;
            
            if (count >= limit) {
                long resetTime = getResetTime();
                return RateLimitResult.builder()
                    .allowed(false)
                    .limit(limit)
                    .remaining(0)
                    .resetTime(resetTime)
                    .message("请求过于频繁，请稍后再试")
                    .build();
            }
            
            // 增加计数器
            long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount == 1) {
                // 第一次设置，设置过期时间为1秒
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }
            
            return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(limit - newCount)
                .resetTime(getResetTime())
                .message("请求成功")
                .build();
        });
    }
    
    /**
     * 检查用户限流
     */
    public Mono<RateLimitResult> checkUserRateLimit(ServerWebExchange exchange, String userId, long limit) {
        return Mono.fromCallable(() -> {
            String key = redisPrefix + "user:" + userId;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            long count = currentCount != null ? Long.parseLong(currentCount) : 0;
            
            if (count >= limit) {
                long resetTime = getResetTime();
                return RateLimitResult.builder()
                    .allowed(false)
                    .limit(limit)
                    .remaining(0)
                    .resetTime(resetTime)
                    .message("用户请求过于频繁，请稍后再试")
                    .build();
            }
            
            // 增加计数器
            long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount == 1) {
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }
            
            return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(limit - newCount)
                .resetTime(getResetTime())
                .message("请求成功")
                .build();
        });
    }
    
    /**
     * 检查API端点限流
     */
    public Mono<RateLimitResult> checkApiRateLimit(ServerWebExchange exchange, String apiPath, long limit) {
        return Mono.fromCallable(() -> {
            String clientIp = extractClientIp(exchange);
            String method = exchange.getRequest().getMethod() != null ? 
                exchange.getRequest().getMethod().name() : "GET";
            String key = redisPrefix + "api:" + method + ":" + apiPath + ":" + clientIp;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            long count = currentCount != null ? Long.parseLong(currentCount) : 0;
            
            if (count >= limit) {
                long resetTime = getResetTime();
                return RateLimitResult.builder()
                    .allowed(false)
                    .limit(limit)
                    .remaining(0)
                    .resetTime(resetTime)
                    .message("API访问过于频繁，请稍后再试")
                    .build();
            }
            
            // 增加计数器
            long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount == 1) {
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }
            
            return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(limit - newCount)
                .resetTime(getResetTime())
                .message("请求成功")
                .build();
        });
    }
    
    /**
     * 检查租户限流
     */
    public Mono<RateLimitResult> checkTenantRateLimit(ServerWebExchange exchange, String tenantId, long limit) {
        return Mono.fromCallable(() -> {
            String key = redisPrefix + "tenant:" + tenantId;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            long count = currentCount != null ? Long.parseLong(currentCount) : 0;
            
            if (count >= limit) {
                long resetTime = getResetTime();
                return RateLimitResult.builder()
                    .allowed(false)
                    .limit(limit)
                    .remaining(0)
                    .resetTime(resetTime)
                    .message("租户请求过于频繁，请稍后再试")
                    .build();
            }
            
            // 增加计数器
            long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount == 1) {
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }
            
            return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(limit - newCount)
                .resetTime(getResetTime())
                .message("请求成功")
                .build();
        });
    }
    
    /**
     * 令牌桶限流算法
     */
    public Mono<RateLimitResult> checkTokenBucketLimit(ServerWebExchange exchange, String bucketId, 
                                                       long capacity, long refillRate) {
        return Mono.fromCallable(() -> {
            try {
                String key = redisPrefix + "token_bucket:" + bucketId;
                
                // 获取当前令牌数
                String tokenCountStr = redisTemplate.opsForValue().get(key + ":tokens");
                String lastRefillStr = redisTemplate.opsForValue().get(key + ":last_refill");
                
                long currentTokens = tokenCountStr != null ? Long.parseLong(tokenCountStr) : capacity;
                long lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : System.currentTimeMillis();
                
                // 计算需要添加的令牌数
                long now = System.currentTimeMillis();
                long timePassed = (now - lastRefillTime) / 1000; // 秒
                long tokensToAdd = timePassed * refillRate;
                
                currentTokens = Math.min(capacity, currentTokens + tokensToAdd);
                
                if (currentTokens >= 1) {
                    // 消耗一个令牌
                    currentTokens--;
                    
                    // 更新令牌数和最后补充时间
                    redisTemplate.opsForValue().set(key + ":tokens", String.valueOf(currentTokens), 60, TimeUnit.SECONDS);
                    redisTemplate.opsForValue().set(key + ":last_refill", String.valueOf(now), 60, TimeUnit.SECONDS);
                    
                    return RateLimitResult.builder()
                        .allowed(true)
                        .limit(capacity)
                        .remaining(currentTokens)
                        .resetTime(now + 1000)
                        .message("令牌桶限流 - 请求成功")
                        .build();
                } else {
                    // 计算下次令牌补充时间
                    long tokensNeeded = 1 - currentTokens;
                    long waitTime = (tokensNeeded + refillRate - 1) / refillRate; // 向上取整
                    long resetTime = now + (waitTime * 1000);
                    
                    return RateLimitResult.builder()
                        .allowed(false)
                        .limit(capacity)
                        .remaining(0)
                        .resetTime(resetTime)
                        .message("令牌桶限流 - 令牌不足，请稍后再试")
                        .build();
                }
            } catch (Exception e) {
                log.error("令牌桶限流检查失败", e);
                // 限流失败时允许通过，避免影响正常业务
                return RateLimitResult.builder()
                    .allowed(true)
                    .limit(-1)
                    .remaining(-1)
                    .resetTime(System.currentTimeMillis() + 1000)
                    .message("令牌桶限流 - 服务异常，默认放行")
                    .build();
            }
        });
    }
    
    /**
     * 滑动窗口限流算法
     */
    public Mono<RateLimitResult> checkSlidingWindowLimit(ServerWebExchange exchange, String windowId, 
                                                         long windowSize, long limit) {
        return Mono.fromCallable(() -> {
            try {
                long now = System.currentTimeMillis();
                long windowStart = now - (windowSize * 1000); // 窗口大小（毫秒）
                
                String key = redisPrefix + "sliding_window:" + windowId;
                
                // 清理过期数据
                redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
                
                // 统计当前窗口内的请求数
                Long currentCount = redisTemplate.opsForZSet().count(key, windowStart, now);
                
                if (currentCount != null && currentCount >= limit) {
                    return RateLimitResult.builder()
                        .allowed(false)
                        .limit(limit)
                        .remaining(0)
                        .resetTime(now + (windowSize * 1000))
                        .message("滑动窗口限流 - 请求过于频繁，请稍后再试")
                        .build();
                }
                
                // 添加当前请求到窗口
                redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
                redisTemplate.expire(key, windowSize + 10, TimeUnit.SECONDS); // 多留10秒缓冲时间
                
                return RateLimitResult.builder()
                    .allowed(true)
                    .limit(limit)
                    .remaining(limit - (currentCount != null ? currentCount + 1 : 1))
                    .resetTime(now + (windowSize * 1000))
                    .message("滑动窗口限流 - 请求成功")
                    .build();
            } catch (Exception e) {
                log.error("滑动窗口限流检查失败", e);
                return RateLimitResult.builder()
                    .allowed(true)
                    .limit(-1)
                    .remaining(-1)
                    .resetTime(System.currentTimeMillis() + 1000)
                    .message("滑动窗口限流 - 服务异常，默认放行")
                    .build();
            }
        });
    }
    
    /**
     * 获取限流配置
     */
    public RateLimitConfig getRateLimitConfig(ServerWebExchange exchange, String path) {
        // 这里可以根据路径、业务模块等获取不同的限流配置
        // 简化实现，返回默认配置
        return RateLimitConfig.builder()
            .enableRateLimit(true)
            .ipLimit(defaultRps)
            .userLimit(defaultTps)
            .apiLimit(200) // API端点默认限制
            .tenantLimit(defaultTps * 10) // 租户默认限制
            .build();
    }
    
    /**
     * 提取客户端IP
     */
    private String extractClientIp(ServerWebExchange exchange) {
        // 优先从代理头获取真实IP
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null 
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
    
    /**
     * 获取重置时间（当前时间的下一秒）
     */
    private long getResetTime() {
        return System.currentTimeMillis() + 1000;
    }
    
    /**
     * 重置限流计数器
     */
    public Mono<Void> resetRateLimit(String type, String identifier) {
        return Mono.fromRunnable(() -> {
            String key = redisPrefix + type + ":" + identifier;
            redisTemplate.delete(key);
            log.info("重置限流计数器 | type={} | identifier={}", type, identifier);
        });
    }
    
    /**
     * 获取当前限流状态
     */
    public Mono<RateLimitStatus> getRateLimitStatus(String type, String identifier) {
        return Mono.fromCallable(() -> {
            String key = redisPrefix + type + ":" + identifier;
            String currentCount = redisTemplate.opsForValue().get(key);
            
            return RateLimitStatus.builder()
                .type(type)
                .identifier(identifier)
                .currentCount(currentCount != null ? Long.parseLong(currentCount) : 0L)
                .build();
        });
    }
}