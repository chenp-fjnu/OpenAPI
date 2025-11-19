package com.group.gateway.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网关认证模块启动类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class GatewayAuthApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayAuthApplication.class, args);
        System.out.println("Gateway Auth Service started successfully!");
    }
    
    /**
     * 内部静态类用于打印启动信息
     */
    public static class GatewayInfo {
        public static void printStartupInfo() {
            System.out.println("===============================================");
            System.out.println("           Gateway Authentication Service");
            System.out.println("           Version: 1.0.0");
            System.out.println("           Status: Running");
            System.out.println("           Features: JWT, OAuth2, SSO, MFA");
            System.out.println("===============================================");
        }
    }
}