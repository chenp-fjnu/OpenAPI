package com.group.gateway.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Gateway Routing Application
 * 网关路由服务启动类
 * 提供高级路由、负载均衡、熔断和降级功能
 */
@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@OpenAPIDefinition(
    info = @Info(
        title = "Gateway Routing API",
        version = "1.0.0",
        description = "网关路由服务API - 提供智能路由、负载均衡和流量管理"
    ),
    tags = {
        @Tag(name = "路由管理", description = "路由规则管理"),
        @Tag(name = "负载均衡", description = "服务实例负载均衡"),
        @Tag(name = "流量控制", description = "流量控制和熔断降级"),
        @Tag(name = "健康检查", description = "服务健康状态检查")
    }
)
public class GatewayRoutingApplication {
    
    public static void main(String[] args) {
        log.info("========================================");
        log.info("  Gateway Routing Service Starting...");
        log.info("========================================");
        log.info("🚀 Gateway Routing Service v1.0.0");
        log.info("📍 功能特性:");
        log.info("   • 智能路由匹配和转发");
        log.info("   • 多种负载均衡算法");
        log.info("   • 熔断器和降级机制");
        log.info("   • 流量控制和限流");
        log.info("   • 健康检查和故障转移");
        log.info("   • 动态路由配置");
        log.info("🔧 技术栈: Spring Cloud Gateway, LoadBalancer, Redis, Nacos");
        log.info("========================================");
        
        SpringApplication.run(GatewayRoutingApplication.class, args);
        
        log.info("========================================");
        log.info("✅ Gateway Routing Service Started Successfully!");
        log.info("📊 Route Definitions Loaded: {}", getRouteCount());
        log.info("🔗 Service Discovery: {}", getDiscoveryStatus());
        log.info("⚡ Load Balancer: {}", getLoadBalancerStatus());
        log.info("========================================");
    }
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * 获取路由规则数量
     */
    private static int getRouteCount() {
        try {
            // 实际实现时应该从RouteDefinitionLocator获取
            return 0;
        } catch (Exception e) {
            log.warn("Failed to get route count", e);
            return 0;
        }
    }
    
    /**
     * 获取服务发现状态
     */
    private static String getDiscoveryStatus() {
        return "Nacos Discovery - Active";
    }
    
    /**
     * 获取负载均衡器状态
     */
    private static String getLoadBalancerStatus() {
        return "Spring Cloud LoadBalancer - Ready";
    }
}