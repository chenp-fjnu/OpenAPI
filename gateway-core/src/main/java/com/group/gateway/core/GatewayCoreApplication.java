package com.group.gateway.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网关核心模块主应用程序
 * 
 * 功能特性：
 * 1. 服务发现与注册
 * 2. 路由配置管理
 * 3. 限流与熔断
 * 4. 认证与授权
 * 5. 请求追踪与监控
 * 6. 缓存管理
 * 7. 定时任务
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.group.gateway.core",
        "com.group.gateway.common"
    }
)
@EnableDiscoveryClient
@EnableCaching
@EnableAsync
@EnableScheduling
public class GatewayCoreApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GatewayCoreApplication.class);
        
        // 设置启动参数
        app.setLogStartupInfo(true);
        app.setAdditionalProfiles("default");
        
        // 启动应用
        app.run(args);
    }
    
    /**
     * 网关配置信息
     */
    private static class GatewayInfo {
        private final GatewayProperties properties;
        private final RouteDefinitionLocator locator;
        
        public GatewayInfo(GatewayProperties properties, RouteDefinitionLocator locator) {
            this.properties = properties;
            this.locator = locator;
        }
        
        public void printGatewayInfo() {
            try {
                System.out.println("╔══════════════════════════════════════════════════════════════╗");
                System.out.println("║                    网关核心模块启动成功                      ║");
                System.out.println("╠══════════════════════════════════════════════════════════════╣");
                System.out.println("║  模块名称: Gateway Core Module                               ║");
                System.out.println("║  版本信息: 1.0.0                                             ║");
                System.out.println("║  启动时间: " + java.time.LocalDateTime.now() + "           ║");
                System.out.println("║  服务端口: ${server.port:8080}                                ║");
                System.out.println("║  注册中心: Spring Cloud Discovery                            ║");
                System.out.println("║  配置中心: Spring Cloud Config                               ║");
                System.out.println("║  限流功能: 启用                                               ║");
                System.out.println("║  熔断功能: 启用                                               ║");
                System.out.println("║  认证功能: 启用                                               ║");
                System.out.println("║  监控功能: 启用                                               ║");
                System.out.println("╚══════════════════════════════════════════════════════════════╝");
            } catch (Exception e) {
                // 静默处理，避免影响启动
            }
        }
    }
}