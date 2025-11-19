package com.group.gateway.routing.controller;

import com.group.gateway.routing.entity.RouteDefinitionEntity;
import com.group.gateway.routing.service.RoutingService;
import com.group.gateway.routing.service.RoutingStatistics;
import com.group.gateway.routing.config.RoutingProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Routing Controller
 * 路由管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/routing")
@RequiredArgsConstructor
@Validated
@Tag(name = "路由管理", description = "网关路由相关API")
public class RoutingController {

    private final RoutingService routingService;
    private final DiscoveryClient discoveryClient;
    private final RoutingStatistics routingStatistics;
    private final RoutingProperties routingProperties;

    @GetMapping("/routes")
    @Operation(summary = "获取路由列表", description = "获取所有路由定义列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取路由列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getRoutes(
            @Parameter(description = "服务名称过滤", required = false)
            @RequestParam(required = false) String serviceName,
            @Parameter(description = "是否只返回活跃路由", required = false)
            @RequestParam(defaultValue = "false") boolean onlyActive) {
        try {
            List<RouteDefinitionEntity> routes = routingService.getRoutes();
            
            // 过滤条件
            if (serviceName != null && !serviceName.isEmpty()) {
                routes = routes.stream()
                    .filter(route -> route.getUri().toString().contains(serviceName))
                    .collect(ArrayList::new, (list, route) -> list.add(route), ArrayList::addAll);
            }
            
            if (onlyActive) {
                routes = routes.stream()
                    .filter(route -> RouteDefinitionEntity.RouteStatus.ACTIVE.equals(route.getStatus()))
                    .collect(ArrayList::new, (list, route) -> list.add(route), ArrayList::addAll);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("routes", routes);
            result.put("total", routes.size());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取路由列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取路由列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/routes/{routeId}")
    @Operation(summary = "获取路由详情", description = "根据路由ID获取路由详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取路由详情"),
        @ApiResponse(responseCode = "404", description = "路由不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getRoute(
            @Parameter(description = "路由ID", required = true, example = "user-service-route")
            @PathVariable @NotBlank String routeId) {
        try {
            Optional<RouteDefinitionEntity> route = routingService.getRoute(routeId);
            
            if (route.isPresent()) {
                return ResponseEntity.ok(route.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "路由不存在: " + routeId));
            }
        } catch (Exception e) {
            log.error("获取路由详情失败: {}", routeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取路由详情失败: " + e.getMessage()));
        }
    }

    @PostMapping("/routes")
    @Operation(summary = "创建路由", description = "创建新的路由定义")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "路由创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public CompletableFuture<ResponseEntity<?>> createRoute(
            @Valid @RequestBody RouteDefinitionEntity routeDefinition) {
        try {
            log.info("收到创建路由请求: {}", routeDefinition.getId());
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    boolean success = routingService.addRoute(routeDefinition);
                    
                    if (success) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "路由创建成功");
                        response.put("routeId", routeDefinition.getId());
                        response.put("timestamp", System.currentTimeMillis());
                        
                        return ResponseEntity.status(HttpStatus.CREATED).body(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "路由创建失败"));
                    }
                } catch (Exception e) {
                    log.error("创建路由失败: {}", routeDefinition.getId(), e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "创建路由失败: " + e.getMessage()));
                }
            });
        } catch (Exception e) {
            log.error("处理创建路由请求异常", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "处理请求异常: " + e.getMessage()))
            );
        }
    }

    @PutMapping("/routes/{routeId}")
    @Operation(summary = "更新路由", description = "更新现有路由定义")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "路由更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "404", description = "路由不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public CompletableFuture<ResponseEntity<?>> updateRoute(
            @Parameter(description = "路由ID", required = true, example = "user-service-route")
            @PathVariable @NotBlank String routeId,
            @Valid @RequestBody RouteDefinitionEntity routeDefinition) {
        try {
            log.info("收到更新路由请求: {}", routeId);
            
            // 确保路由ID匹配
            if (!routeId.equals(routeDefinition.getId())) {
                return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "路径中的路由ID与请求体中的路由ID不匹配"))
                );
            }
            
            return CompletableFuture.supplyAsync(() -> {
                try {
                    boolean success = routingService.updateRoute(routeId, routeDefinition);
                    
                    if (success) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("message", "路由更新成功");
                        response.put("routeId", routeId);
                        response.put("timestamp", System.currentTimeMillis());
                        
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "路由不存在: " + routeId));
                    }
                } catch (Exception e) {
                    log.error("更新路由失败: {}", routeId, e);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "更新路由失败: " + e.getMessage()));
                }
            });
        } catch (Exception e) {
            log.error("处理更新路由请求异常", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "处理请求异常: " + e.getMessage()))
            );
        }
    }

    @DeleteMapping("/routes/{routeId}")
    @Operation(summary = "删除路由", description = "根据路由ID删除路由")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "路由删除成功"),
        @ApiResponse(responseCode = "404", description = "路由不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> deleteRoute(
            @Parameter(description = "路由ID", required = true, example = "user-service-route")
            @PathVariable @NotBlank String routeId) {
        try {
            log.info("收到删除路由请求: {}", routeId);
            
            boolean success = routingService.deleteRoute(routeId);
            
            if (success) {
                Map<String, Object> response = new HashMap<>();
                response.put("message", "路由删除成功");
                response.put("routeId", routeId);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "路由不存在: " + routeId));
            }
        } catch (Exception e) {
            log.error("删除路由失败: {}", routeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "删除路由失败: " + e.getMessage()));
        }
    }

    @PutMapping("/routes/{routeId}/enable")
    @Operation(summary = "启用路由", description = "启用指定路由")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "路由启用成功"),
        @ApiResponse(responseCode = "404", description = "路由不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> enableRoute(
            @Parameter(description = "路由ID", required = true, example = "user-service-route")
            @PathVariable @NotBlank String routeId) {
        try {
            boolean success = routingService.enableRoute(routeId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "路由启用成功",
                    "routeId", routeId,
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "路由不存在: " + routeId));
            }
        } catch (Exception e) {
            log.error("启用路由失败: {}", routeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "启用路由失败: " + e.getMessage()));
        }
    }

    @PutMapping("/routes/{routeId}/disable")
    @Operation(summary = "禁用路由", description = "禁用指定路由")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "路由禁用成功"),
        @ApiResponse(responseCode = "404", description = "路由不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> disableRoute(
            @Parameter(description = "路由ID", required = true, example = "user-service-route")
            @PathVariable @NotBlank String routeId) {
        try {
            boolean success = routingService.disableRoute(routeId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "message", "路由禁用成功",
                    "routeId", routeId,
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "路由不存在: " + routeId));
            }
        } catch (Exception e) {
            log.error("禁用路由失败: {}", routeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "禁用路由失败: " + e.getMessage()));
        }
    }

    @GetMapping("/services")
    @Operation(summary = "获取服务列表", description = "获取所有注册的服务列表")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取服务列表"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getServices() {
        try {
            List<String> services = discoveryClient.getServices();
            
            Map<String, Object> result = new HashMap<>();
            result.put("services", services);
            result.put("total", services.size());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取服务列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取服务列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/services/{serviceId}/instances")
    @Operation(summary = "获取服务实例", description = "获取指定服务的所有实例")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取服务实例"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getServiceInstances(
            @Parameter(description = "服务ID", required = true, example = "user-service")
            @PathVariable String serviceId) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("serviceId", serviceId);
            result.put("instances", instances);
            result.put("total", instances.size());
            result.put("healthyInstances", instances.stream().mapToInt(ServiceInstance::getHealthCheckOk ? 1 : 0).sum());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取服务实例失败: {}", serviceId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取服务实例失败: " + e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取路由统计", description = "获取路由相关的统计信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取统计信息"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getRoutingStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 基本路由统计
            statistics.put("totalRoutes", routingStatistics.getTotalRoutes().get());
            statistics.put("activeRoutes", routingStatistics.getActiveRoutes().get());
            statistics.put("inactiveRoutes", routingStatistics.getInactiveRoutes().get());
            
            // 路由选择统计
            statistics.put("successfulSelections", routingStatistics.getSuccessfulSelections().sum());
            statistics.put("failedSelections", routingStatistics.getFailedSelections().sum());
            statistics.put("selectionSuccessRate", routingStatistics.getSelectionSuccessRate());
            
            // 限流统计
            statistics.put("rateLimitedRequests", routingStatistics.getRateLimitedRequests().sum());
            statistics.put("allowedRequests", routingStatistics.getAllowedRequests().sum());
            
            // 健康检查统计
            statistics.put("healthyInstances", routingStatistics.getHealthyInstances().get());
            statistics.put("unhealthyInstances", routingStatistics.getUnhealthyInstances().get());
            statistics.put("healthCheckSuccessRate", routingStatistics.getHealthCheckSuccessRate());
            
            // 性能统计
            statistics.put("averageResponseTime", routingStatistics.getAverageResponseTime());
            
            statistics.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取路由统计失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取路由统计失败: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新路由", description = "刷新路由配置和实例信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "路由刷新成功"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> refreshRoutes() {
        try {
            routingService.refreshRoutes();
            
            return ResponseEntity.ok(Map.of(
                "message", "路由刷新成功",
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("刷新路由失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "刷新路由失败: " + e.getMessage()));
        }
    }

    @GetMapping("/health-check")
    @Operation(summary = "健康检查", description = "检查路由服务健康状态")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "服务健康"),
        @ApiResponse(responseCode = "503", description = "服务不健康")
    })
    public ResponseEntity<?> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            
            // 检查基本组件状态
            boolean serviceRegistryHealthy = !discoveryClient.getServices().isEmpty();
            boolean loadBalancerHealthy = routingService.getLoadBalancerCache() != null;
            boolean healthCheckHealthy = routingService.getHealthyInstancesCount() >= 0;
            
            boolean overallHealthy = serviceRegistryHealthy && loadBalancerHealthy && healthCheckHealthy;
            
            health.put("status", overallHealthy ? "UP" : "DOWN");
            health.put("components", Map.of(
                "serviceRegistry", serviceRegistryHealthy ? "UP" : "DOWN",
                "loadBalancer", loadBalancerHealthy ? "UP" : "DOWN",
                "healthCheck", healthCheckHealthy ? "UP" : "DOWN"
            ));
            health.put("statistics", Map.of(
                "totalRoutes", routingStatistics.getTotalRoutes().get(),
                "activeRoutes", routingStatistics.getActiveRoutes().get(),
                "healthyInstances", routingStatistics.getHealthyInstances().get()
            ));
            health.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(overallHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "status", "DOWN",
                    "error", "健康检查失败: " + e.getMessage(),
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }

    @GetMapping("/config")
    @Operation(summary = "获取路由配置", description = "获取当前的路由配置信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取配置"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<?> getRoutingConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 全局路由配置
            config.put("global", Map.of(
                "refreshInterval", routingProperties.getGlobal().getRefreshInterval(),
                "cacheExpireTime", routingProperties.getGlobal().getCacheExpireTime(),
                "enableMetrics", routingProperties.getGlobal().isEnableMetrics()
            ));
            
            // 负载均衡配置
            config.put("loadBalancer", Map.of(
                "defaultAlgorithm", routingProperties.getLoadBalancer().getDefaultAlgorithm(),
                "enableHealthCheck", routingProperties.getLoadBalancer().isEnableHealthCheck(),
                "healthCheckInterval", routingProperties.getLoadBalancer().getHealthCheckInterval()
            ));
            
            // 熔断器配置
            config.put("circuitBreaker", Map.of(
                "failureRateThreshold", routingProperties.getCircuitBreaker().getFailureRateThreshold(),
                "requestVolumeThreshold", routingProperties.getCircuitBreaker().getRequestVolumeThreshold(),
                "waitDurationInOpenState", routingProperties.getCircuitBreaker().getWaitDurationInOpenState()
            ));
            
            // 限流配置
            config.put("rateLimit", Map.of(
                "enabled", routingProperties.getRateLimit().isEnabled(),
                "defaultRate", routingProperties.getRateLimit().getDefaultRate(),
                "burstCapacity", routingProperties.getRateLimit().getBurstCapacity()
            ));
            
            config.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(config);
        } catch (Exception e) {
            log.error("获取路由配置失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "获取路由配置失败: " + e.getMessage()));
        }
    }
}