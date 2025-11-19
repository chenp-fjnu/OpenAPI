package com.group.gateway.ratelimit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 限流模块主应用程序
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class GatewayRateLimitApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayRateLimitApplication.class, args);
        System.out.println("\n" + GatewayInfo.getGatewayInfo());
    }

    /**
     * 网关信息内部类
     */
    public static class GatewayInfo {
        public static String getGatewayInfo() {
            return """
                    
                    ================================================
                        集团API网关 - 限流模块启动成功
                    ================================================
                    模块名称: gateway-ratelimit
                    版本信息: v1.0.0
                    功能特性:
                    ✓ 令牌桶限流算法
                    ✓ 滑动时间窗口限流
                    ✓ 固定时间窗口限流
                    ✓ 基于Redis的分布式限流
                    ✓ 多维度限流策略（IP、用户、API、租户）
                    ✓ 限流规则动态配置
                    ✓ 限流指标监控
                    ✓ 限流降级策略
                    
                    技术栈:
                    - Spring Boot 3.2.0
                    - Spring Cloud Gateway 4.1.0
                    - Redis分布式缓存
                    - Nacos服务发现和配置中心
                    - Micrometer Prometheus监控
                    
                    ================================================
                    """;
        }
    }
}