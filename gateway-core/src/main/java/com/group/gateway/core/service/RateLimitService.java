package com.group.gateway.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 限流服务实现
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class RateLimitService {
    
    private static final String REDIS_PREFIX = "rate_limit:";
    private static final String DEFAULT_WINDOW = "60"; // 60秒窗口
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private RequestContextService requestContextService;
    
    @Autowired
    private ClientInfoService clientInfoService;
    
    @Value("${gateway.rate.limit.default.enabled:true}")
    private boolean rateLimitEnabled;
    
    @Value("${gateway.rate.limit.default.window:60}")
    private long defaultWindow;
    
    @Value("${gateway.rate.limit.default.requests:100}")
    private long defaultRequests;
    
    @Value("${gateway.rate.limit.ip.enabled:true}")
    private boolean ipRateLimitEnabled;
    
    @Value("${gateway.rate.limit.ip.requests:1000}")
    private long ipRateLimitRequests;
    
    @Value("${gateway.rate.limit.user.enabled:true}")
    private boolean userRateLimitEnabled;
    
    @Value("${gateway.rate.limit.user.requests:100}")
    private long userRateLimitRequests;
    
    @Value("${gateway.rate.limit.api.enabled:true}")
    private boolean apiRateLimitEnabled;
    
    @Value("${gateway.rate.limit.api.requests:10000}")
    private long apiRateLimitRequests;
    
    @Value("${gateway.rate.limit.tenant.enabled:true}")
    private boolean tenantRateLimitEnabled;
    
    @Value("${gateway.rate.limit.tenant.requests:1000}")
    private long tenantRateLimitRequests;
    
    /**
     * IP限流检查
     */
    public Mono<RateLimitInfo> checkIpRateLimit(ServerWebExchange exchange) {
        if (!ipRateLimitEnabled) {
            return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.IP, "IP限流已禁用"));
        }
        
        return clientInfoService.getClientInfo(exchange)
            .flatMap(clientInfo -> {
                String ip = clientInfo.getIp();
                long limit = getEffectiveIpLimit(clientInfo);
                
                return checkRateLimit(ip, limit, defaultWindow, RateLimitInfo.LimitType.IP, "客户端IP限流")
                    .map(limitInfo -> {
                        limitInfo.setLimitKey(ip);
                        return limitInfo;
                    });
            })
            .onErrorResume(e -> {
                log.error("IP限流检查失败", e);
                return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.IP, "限流检查异常，允许请求"));
            });
    }
    
    /**
     * 用户限流检查
     */
    public Mono<RateLimitInfo> checkUserRateLimit(ServerWebExchange exchange) {
        if (!userRateLimitEnabled) {
            return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.USER, "用户限流已禁用"));
        }
        
        return requestContextService.getAuthInfo(exchange)
            .flatMap(authInfo -> {
                if (authInfo.getUserId() == null) {
                    // 没有用户ID，跳过用户限流
                    return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.USER, "匿名用户跳过限流"));
                }
                
                String userKey = "user:" + authInfo.getUserId();
                long limit = getEffectiveUserLimit(authInfo);
                
                return checkRateLimit(userKey, limit, defaultWindow, RateLimitInfo.LimitType.USER, "用户限流")
                    .map(limitInfo -> {
                        limitInfo.setLimitKey(userKey);
                        return limitInfo;
                    });
            })
            .switchIfEmpty(Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.USER, "匿名用户跳过限流")))
            .onErrorResume(e -> {
                log.error("用户限流检查失败", e);
                return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.USER, "限流检查异常，允许请求"));
            });
    }
    
    /**
     * API限流检查
     */
    public Mono<RateLimitInfo> checkApiRateLimit(ServerWebExchange exchange) {
        if (!apiRateLimitEnabled) {
            return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.API, "API限流已禁用"));
        }
        
        String apiKey = getApiKey(exchange);
        long limit = getEffectiveApiLimit();
        
        return checkRateLimit(apiKey, limit, defaultWindow, RateLimitInfo.LimitType.API, "API端点限流")
            .map(limitInfo -> {
                limitInfo.setLimitKey(apiKey);
                return limitInfo;
            })
            .onErrorResume(e -> {
                log.error("API限流检查失败", e);
                return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.API, "限流检查异常，允许请求"));
            });
    }
    
    /**
     * 租户限流检查
     */
    public Mono<RateLimitInfo> checkTenantRateLimit(ServerWebExchange exchange) {
        if (!tenantRateLimitEnabled) {
            return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.TENANT, "租户限流已禁用"));
        }
        
        return requestContextService.getAuthInfo(exchange)
            .flatMap(authInfo -> {
                String tenantKey = authInfo.getTenantId();
                if (tenantKey == null) {
                    return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.TENANT, "无租户信息跳过限流"));
                }
                
                long limit = getEffectiveTenantLimit(authInfo);
                
                return checkRateLimit(tenantKey, limit, defaultWindow, RateLimitInfo.LimitType.TENANT, "租户限流")
                    .map(limitInfo -> {
                        limitInfo.setLimitKey(tenantKey);
                        return limitInfo;
                    });
            })
            .switchIfEmpty(Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.TENANT, "无租户信息跳过限流")))
            .onErrorResume(e -> {
                log.error("租户限流检查失败", e);
                return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.TENANT, "限流检查异常，允许请求"));
            });
    }
    
    /**
     * 综合限流检查
     */
    public Mono<RateLimitInfo> checkRateLimit(ServerWebExchange exchange) {
        List<Mono<RateLimitInfo>> checks = new ArrayList<>();
        
        // IP限流
        checks.add(checkIpRateLimit(exchange));
        
        // 用户限流
        checks.add(checkUserRateLimit(exchange));
        
        // API限流
        checks.add(checkApiRateLimit(exchange));
        
        // 租户限流
        checks.add(checkTenantRateLimit(exchange));
        
        return Mono.when(checks)
            .flatMap(results -> {
                // 找出第一个触发的限流
                for (RateLimitInfo limitInfo : results) {
                    if (limitInfo.isLimited()) {
                        return Mono.just(limitInfo);
                    }
                }
                
                // 所有检查都通过，返回第一个作为主限流信息
                return Mono.just(results.get(0));
            })
            .onErrorResume(e -> {
                log.error("综合限流检查失败", e);
                return Mono.just(createAllowedLimitInfo(RateLimitInfo.LimitType.UNKNOWN, "限流检查异常，允许请求"));
            });
    }
    
    /**
     * 执行限流检查（基于Redis的滑动窗口）
     */
    private Mono<RateLimitInfo> checkRateLimit(String key, long limit, long windowSeconds, RateLimitInfo.LimitType type, String description) {
        return Mono.fromCallable(() -> {
            try {
                String redisKey = REDIS_PREFIX + type.name().toLowerCase() + ":" + key;
                long currentTime = Instant.now().getEpochSecond();
                long windowStart = currentTime - windowSeconds;
                
                // 使用Redis Sorted Set实现滑动窗口
                redisTemplate.opsForZSet().removeRangeByScore(redisKey, 0, windowStart);
                
                // 获取当前窗口内的请求数
                Long currentCount = redisTemplate.opsForZSet().count(redisKey, currentTime - windowSeconds, currentTime);
                if (currentCount == null) {
                    currentCount = 0L;
                }
                
                if (currentCount >= limit) {
                    // 触发限流
                    RateLimitInfo limitInfo = createLimitedLimitInfo(type, key, currentCount, limit, windowSeconds, description);
                    log.warn("触发限流 - 类型: {}, 键: {}, 当前: {}, 限制: {}", type, key, currentCount, limit);
                    return limitInfo;
                } else {
                    // 允许请求，添加新请求
                    redisTemplate.opsForZSet().add(redisKey, String.valueOf(currentTime), currentTime);
                    redisTemplate.expire(redisKey, windowSeconds * 2, TimeUnit.SECONDS);
                    
                    RateLimitInfo limitInfo = createAllowedLimitInfo(type, description);
                    limitInfo.setCurrentCount(currentCount + 1);
                    limitInfo.setThreshold(limit);
                    limitInfo.setLimitKey(key);
                    return limitInfo;
                }
            } catch (Exception e) {
                log.error("限流检查异常 - 键: {}, 类型: {}", key, type, e);
                return createAllowedLimitInfo(type, "限流检查异常，允许请求");
            }
        });
    }
    
    /**
     * 创建允许的限流信息
     */
    private RateLimitInfo createAllowedLimitInfo(RateLimitInfo.LimitType type, String reason) {
        RateLimitInfo limitInfo = new RateLimitInfo();
        limitInfo.setLimitType(type);
        limitInfo.setLimited(false);
        limitInfo.setReason(reason);
        limitInfo.setAlgorithmType(RateLimitInfo.AlgorithmType.SLIDING_WINDOW);
        limitInfo.setPriority(RateLimitInfo.Priority.NORMAL);
        return limitInfo;
    }
    
    /**
     * 创建触发限流的信息
     */
    private RateLimitInfo createLimitedLimitInfo(RateLimitInfo.LimitType type, String key, long currentCount, long limit, long windowSeconds, String reason) {
        RateLimitInfo limitInfo = new RateLimitInfo();
        limitInfo.setLimitType(type);
        limitInfo.setLimited(true);
        limitInfo.setLimitKey(key);
        limitInfo.setCurrentCount(currentCount);
        limitInfo.setThreshold(limit);
        limitInfo.setReason(reason);
        limitInfo.setAlgorithmType(RateLimitInfo.AlgorithmType.SLIDING_WINDOW);
        limitInfo.setPriority(RateLimitInfo.Priority.HIGH);
        limitInfo.setRemainingTime(windowSeconds);
        
        // 构建详细的限流描述
        String description = String.format("%s - 当前请求数(%d)超过限制(%d)，%d秒后重试", 
            reason, currentCount, limit, windowSeconds);
        limitInfo.setDescription(description);
        
        return limitInfo;
    }
    
    /**
     * 获取API密钥
     */
    private String getApiKey(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        return method + ":" + path;
    }
    
    /**
     * 获取有效的IP限流值
     */
    private long getEffectiveIpLimit(ClientInfo clientInfo) {
        // 移动设备可以设置更宽松的限制
        if (clientInfo.getDeviceType() == ClientInfo.DeviceType.MOBILE) {
            return ipRateLimitRequests * 2;
        }
        
        // 可信IP可以设置更宽松的限制
        if (clientInfo.isTrusted()) {
            return ipRateLimitRequests * 5;
        }
        
        return ipRateLimitRequests;
    }
    
    /**
     * 获取有效的用户限流值
     */
    private long getEffectiveUserLimit(RateLimitInfo.AuthInfo authInfo) {
        // 根据用户角色调整限制
        if (authInfo.getRoles() != null && authInfo.getRoles().contains("premium")) {
            return userRateLimitRequests * 3;
        }
        
        return userRateLimitRequests;
    }
    
    /**
     * 获取有效的API限流值
     */
    private long getEffectiveApiLimit() {
        return apiRateLimitRequests;
    }
    
    /**
     * 获取有效的租户限流值
     */
    private long getEffectiveTenantLimit(RateLimitInfo.AuthInfo authInfo) {
        // 根据租户等级调整限制
        return tenantRateLimitRequests;
    }
    
    /**
     * 重置限流计数器
     */
    public void resetRateLimit(String key, RateLimitInfo.LimitType type) {
        try {
            String redisKey = REDIS_PREFIX + type.name().toLowerCase() + ":" + key;
            redisTemplate.delete(redisKey);
            log.debug("重置限流计数器 - 键: {}, 类型: {}", key, type);
        } catch (Exception e) {
            log.error("重置限流计数器失败 - 键: {}, 类型: {}", key, type, e);
        }
    }
    
    /**
     * 获取限流状态
     */
    public Map<String, Object> getRateLimitStatus(String key, RateLimitInfo.LimitType type) {
        try {
            String redisKey = REDIS_PREFIX + type.name().toLowerCase() + ":" + key;
            Long count = redisTemplate.opsForZSet().count(redisKey, 
                Instant.now().minusSeconds(defaultWindow).getEpochSecond(), 
                Instant.now().getEpochSecond());
            
            Map<String, Object> status = new HashMap<>();
            status.put("key", key);
            status.put("type", type.name());
            status.put("currentCount", count != null ? count : 0);
            status.put("threshold", getThresholdByType(type));
            status.put("windowSeconds", defaultWindow);
            status.put("isLimited", count != null && count >= getThresholdByType(type));
            
            return status;
        } catch (Exception e) {
            log.error("获取限流状态失败 - 键: {}, 类型: {}", key, type, e);
            return Map.of("error", e.getMessage());
        }
    }
    
    /**
     * 根据类型获取阈值
     */
    private long getThresholdByType(RateLimitInfo.LimitType type) {
        return switch (type) {
            case IP -> ipRateLimitRequests;
            case USER -> userRateLimitRequests;
            case API -> apiRateLimitRequests;
            case TENANT -> tenantRateLimitRequests;
            default -> defaultRequests;
        };
    }
    
    /**
     * 获取限流统计信息
     */
    public Map<String, Object> getRateLimitStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", rateLimitEnabled);
        stats.put("defaults", Map.of(
            "window", defaultWindow,
            "requests", defaultRequests
        ));
        stats.put("types", Map.of(
            "ip", Map.of("enabled", ipRateLimitEnabled, "requests", ipRateLimitRequests),
            "user", Map.of("enabled", userRateLimitEnabled, "requests", userRateLimitRequests),
            "api", Map.of("enabled", apiRateLimitEnabled, "requests", apiRateLimitRequests),
            "tenant", Map.of("enabled", tenantRateLimitEnabled, "requests", tenantRateLimitRequests)
        ));
        
        return stats;
    }
}