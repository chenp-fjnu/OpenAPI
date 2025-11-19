package com.group.gateway.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 网关日志模块主应用程序
 * 
 * 功能特性：
 * - 分布式日志收集和分析
 * - 多种日志存储方式（文件、数据库、ELK）
 * - 实时日志流处理
 * - 日志聚合和搜索
 * - 日志清理和归档
 * - 异常日志告警
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class GatewayLoggingApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(GatewayLoggingApplication.class);
        
        // 添加Banner关闭选项
        app.setBannerMode(Banner.Mode.OFF);
        
        // 启动应用
        ConfigurableApplicationContext context = app.run(args);
        
        // 输出启动信息
        printStartupInfo(context);
    }
    
    /**
     * 打印启动信息
     */
    private static void printStartupInfo(ConfigurableApplicationContext context) {
        String banner = """
                
                ╔══════════════════════════════════════════════════════════════════╗
                ║                     集团级API网关 - 日志模块                      ║
                ║                              v1.0.0                             ║
                ╠══════════════════════════════════════════════════════════════════╣
                ║                                                                      ║
                ║  🚀 日志模块已成功启动                                              ║
                ║                                                                      ║
                ║  📋 功能特性：                                                    ║
                ║     • 分布式日志收集                                               ║
                ║     • 多存储方式支持                                               ║
                ║     • 实时日志流处理                                               ║
                ║     • 日志聚合搜索                                                 ║
                ║     • 异常告警通知                                                 ║
                ║                                                                      ║
                ║  🔧 技术栈：                                                       ║
                ║     • Spring Boot 3.x                                             ║
                ║     • ELK Stack                                                   ║
                ║     • Redis缓存                                                   ║
                ║     • Kafka消息队列                                               ║
                ║                                                                      ║
                ╚══════════════════════════════════════════════════════════════════╝
                """;
        
        System.out.println(banner);
    }
    
    /**
     * 应用信息静态内部类
     */
    public static class GatewayInfo {
        public static final String VERSION = "1.0.0";
        public static final String NAME = "Gateway Logging Module";
        public static final String DESCRIPTION = "集团级API网关日志模块";
        
        /**
         * 获取应用版本
         */
        public static String getVersion() {
            return VERSION;
        }
        
        /**
         * 获取应用名称
         */
        public static String getName() {
            return NAME;
        }
        
        /**
         * 获取应用描述
         */
        public static String getDescription() {
            return DESCRIPTION;
        }
    }
}