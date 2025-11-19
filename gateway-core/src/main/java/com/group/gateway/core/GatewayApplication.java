package com.group.gateway.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 网关核心应用启动类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class GatewayApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
        System.out.println("===========================================");
        System.out.println("         集团级API网关启动成功                ");
        System.out.println("         版本: 1.0.0                     ");
        System.out.println("         端口: 8080                      ");
        System.out.println("===========================================");
    }
}