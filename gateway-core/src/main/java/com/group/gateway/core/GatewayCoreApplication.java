package com.group.gateway.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Gateway Core Application
 * 核心网关应用程序入口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayCoreApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GatewayCoreApplication.class, args);
    }
}