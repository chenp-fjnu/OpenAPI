package com.group.gateway.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 监控模块主应用程序
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class GatewayMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayMonitorApplication.class, args);
        System.out.println("\n" + GatewayInfo.getGatewayInfo());
    }

    /**
     * 网关信息内部类
     */
    public static class GatewayInfo {
        public static String getGatewayInfo() {
            return """
                    
                    ================================================
                        集团API网关 - 监控模块启动成功
                    ================================================
                    模块名称: gateway-monitor
                    版本信息: v1.0.0
                    功能特性:
                    ✓ 实时性能指标收集
                    ✓ 请求响应时间监控
                    ✓ 错误率统计和告警
                    ✓ 流量分析和趋势
                    ✓ 链路追踪集成
                    ✓ 动态阈值告警
                    ✓ 可视化监控面板
                    ✓ Prometheus指标集成
                    
                    技术栈:
                    - Spring Boot 3.2.0
                    - Spring Cloud Gateway 4.1.0
                    - Micrometer Prometheus
                    - Redis时序数据存储
                    - Nacos服务发现和配置中心
                    
                    ================================================
                    """;
        }
    }
}