package com.group.gateway.routing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Routing Service
 * 路由服务类
 * 提供智能路由、负载均衡、熔断和流量控制功能
 */
@Slf4j
@Service
public class RoutingService {

    private final DiscoveryClient discoveryClient;
    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RestTemplate restTemplate;
    private final WebClient.Builder webClientBuilder;
    
    // 路由缓存：路由ID -> 路由定义实体
    private final Map<String, RouteDefinitionEntity> routeCache = new ConcurrentHashMap<>();
    
    // 负载均衡器缓存：服务名 -> 实例列表
    private final Map<String, List<ServiceInstance>> serviceInstancesCache = new ConcurrentHashMap<>();
    
    // 健康检查结果：服务名 -> 健康状态
    private final Map<String, Boolean> healthStatusCache = new ConcurrentHashMap<>();
    
    // 限流计数器：键 -> 请求计数
    private final Map<String, RateLimitCounter> rateLimitCounters = new ConcurrentHashMap<>();
    
    // 定时任务执行器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // 统计信息
    private final RoutingStatistics statistics = new RoutingStatistics();

    public RoutingService(DiscoveryClient discoveryClient,
                         RouteDefinitionLocator routeDefinitionLocator,
                         RestTemplate restTemplate,
                         WebClient.Builder webClientBuilder) {
        this.discoveryClient = discoveryClient;
        this.routeDefinitionLocator = routeDefinitionLocator;
        this.restTemplate = restTemplate;
        this.webClientBuilder = webClientBuilder;
        
        // 启动定时任务
        startScheduledTasks();
        
        // 加载初始路由
        loadInitialRoutes();
    }

    /**
     * 启动定时任务
     */
    private void startScheduledTasks() {
        // 定时刷新服务实例
        scheduler.scheduleAtFixedRate(this::refreshServiceInstances, 30, 30, TimeUnit.SECONDS);
        
        // 定时健康检查
        scheduler.scheduleAtFixedRate(this::performHealthChecks, 15, 15, TimeUnit.SECONDS);
        
        // 定时清理限流计数器
        scheduler.scheduleAtFixedRate(this::cleanupRateLimitCounters, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 加载初始路由
     */
    private void loadInitialRoutes() {
        try {
            Flux<RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions();
            routes.subscribe(route -> {
                log.debug("Loaded route: {}", route.getId());
                // 这里应该从数据库或其他存储加载完整的路由配置
            });
        } catch (Exception e) {
            log.warn("Failed to load initial routes", e);
        }
    }

    /**
     * 获取所有路由
     */
    public List<RouteDefinitionEntity> getAllRoutes() {
        return new ArrayList<>(routeCache.values());
    }

    /**
     * 根据ID获取路由
     */
    public Optional<RouteDefinitionEntity> getRouteById(String routeId) {
        return Optional.ofNullable(routeCache.get(routeId));
    }

    /**
     * 添加路由
     */
    public boolean addRoute(RouteDefinitionEntity routeEntity) {
        try {
            log.info("Adding route: {}", routeEntity.getRouteId());
            
            // 验证路由配置
            if (!validateRoute(routeEntity)) {
                log.error("Route validation failed: {}", routeEntity.getRouteId());
                return false;
            }
            
            // 添加到缓存
            routeEntity.setCreatedTime(LocalDateTime.now());
            routeEntity.setUpdatedTime(LocalDateTime.now());
            routeCache.put(routeEntity.getRouteId(), routeEntity);
            
            // 更新统计信息
            statistics.incrementTotalRoutes();
            
            log.info("Successfully added route: {}", routeEntity.getRouteId());
            return true;
        } catch (Exception e) {
            log.error("Failed to add route: {}", routeEntity.getRouteId(), e);
            return false;
        }
    }

    /**
     * 更新路由
     */
    public boolean updateRoute(RouteDefinitionEntity routeEntity) {
        try {
            log.info("Updating route: {}", routeEntity.getRouteId());
            
            if (!routeCache.containsKey(routeEntity.getRouteId())) {
                log.warn("Route not found for update: {}", routeEntity.getRouteId());
                return false;
            }
            
            // 验证路由配置
            if (!validateRoute(routeEntity)) {
                log.error("Route validation failed: {}", routeEntity.getRouteId());
                return false;
            }
            
            // 更新路由
            routeEntity.setUpdatedTime(LocalDateTime.now());
            routeCache.put(routeEntity.getRouteId(), routeEntity);
            
            log.info("Successfully updated route: {}", routeEntity.getRouteId());
            return true;
        } catch (Exception e) {
            log.error("Failed to update route: {}", routeEntity.getRouteId(), e);
            return false;
        }
    }

    /**
     * 删除路由
     */
    public boolean deleteRoute(String routeId) {
        try {
            log.info("Deleting route: {}", routeId);
            
            RouteDefinitionEntity removed = routeCache.remove(routeId);
            if (removed != null) {
                statistics.decrementTotalRoutes();
                log.info("Successfully deleted route: {}", routeId);
                return true;
            } else {
                log.warn("Route not found for deletion: {}", routeId);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to delete route: {}", routeId, e);
            return false;
        }
    }

    /**
     * 启用路由
     */
    public boolean enableRoute(String routeId) {
        return changeRouteStatus(routeId, RouteDefinitionEntity.RouteStatus.ACTIVE);
    }

    /**
     * 禁用路由
     */
    public boolean disableRoute(String routeId) {
        return changeRouteStatus(routeId, RouteDefinitionEntity.RouteStatus.DISABLED);
    }

    /**
     * 变更路由状态
     */
    private boolean changeRouteStatus(String routeId, RouteDefinitionEntity.RouteStatus status) {
        RouteDefinitionEntity route = routeCache.get(routeId);
        if (route != null) {
            route.setStatus(status);
            route.setUpdatedTime(LocalDateTime.now());
            log.info("Route status changed: {} -> {}", routeId, status);
            return true;
        }
        return false;
    }

    /**
     * 智能路由选择
     */
    public Optional<ServiceInstance> selectServiceInstance(String serviceId, String routeId) {
        try {
            log.debug("Selecting service instance for service: {}, route: {}", serviceId, routeId);
            
            RouteDefinitionEntity route = routeCache.get(routeId);
            if (route == null || route.getStatus() != RouteDefinitionEntity.RouteStatus.ACTIVE) {
                log.warn("Route not found or inactive: {}", routeId);
                return Optional.empty();
            }
            
            // 获取健康的服务实例
            List<ServiceInstance> healthyInstances = getHealthyServiceInstances(serviceId);
            if (healthyInstances.isEmpty()) {
                log.warn("No healthy instances found for service: {}", serviceId);
                return Optional.empty();
            }
            
            // 应用负载均衡策略
            ServiceInstance selectedInstance = selectInstanceByLoadBalancer(
                healthyInstances, route.getLoadBalancerConfig());
            
            if (selectedInstance != null) {
                statistics.incrementSuccessfulSelections();
                log.debug("Selected instance: {} for service: {}", selectedInstance.getInstanceId(), serviceId);
            } else {
                statistics.incrementFailedSelections();
            }
            
            return Optional.ofNullable(selectedInstance);
        } catch (Exception e) {
            log.error("Failed to select service instance", e);
            statistics.incrementFailedSelections();
            return Optional.empty();
        }
    }

    /**
     * 按负载均衡策略选择实例
     */
    private ServiceInstance selectInstanceByLoadBalancer(List<ServiceInstance> instances, 
                                                       RouteDefinitionEntity.LoadBalancerConfig config) {
        if (instances.isEmpty()) {
            return null;
        }
        
        if (instances.size() == 1) {
            return instances.get(0);
        }
        
        String algorithm = config != null ? config.getAlgorithm() : "ROUND_ROBIN";
        
        switch (algorithm.toUpperCase()) {
            case "RANDOM":
                return selectRandomInstance(instances);
            case "LEAST_CONNECTIONS":
                return selectLeastConnectionsInstance(instances);
            case "WEIGHTED_RESPONSE_TIME":
                return selectWeightedResponseTimeInstance(instances);
            case "ROUND_ROBIN":
            default:
                return selectRoundRobinInstance(instances);
        }
    }

    private ServiceInstance selectRoundRobinInstance(List<ServiceInstance> instances) {
        int index = statistics.getNextRoundRobinIndex(instances.size());
        return instances.get(index);
    }

    private ServiceInstance selectRandomInstance(List<ServiceInstance> instances) {
        Random random = new Random();
        return instances.get(random.nextInt(instances.size()));
    }

    private ServiceInstance selectLeastConnectionsInstance(List<ServiceInstance> instances) {
        // 简化实现，实际应该检查每个实例的连接数
        return instances.get(0);
    }

    private ServiceInstance selectWeightedResponseTimeInstance(List<ServiceInstance> instances) {
        // 简化实现，实际应该基于响应时间权重选择
        return instances.get(0);
    }

    /**
     * 获取健康的服务实例
     */
    private List<ServiceInstance> getHealthyServiceInstances(String serviceId) {
        List<ServiceInstance> allInstances = serviceInstancesCache.getOrDefault(serviceId, 
            discoveryClient.getInstances(serviceId));
        
        return allInstances.stream()
            .filter(instance -> isInstanceHealthy(serviceId, instance))
            .collect(Collectors.toList());
    }

    /**
     * 检查实例是否健康
     */
    private boolean isInstanceHealthy(String serviceId, ServiceInstance instance) {
        Boolean isHealthy = healthStatusCache.get(serviceId + ":" + instance.getInstanceId());
        return isHealthy != null ? isHealthy : true; // 默认为健康
    }

    /**
     * 刷新服务实例缓存
     */
    private void refreshServiceInstances() {
        try {
            log.debug("Refreshing service instances cache");
            
            discoveryClient.getServices().forEach(serviceId -> {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
                serviceInstancesCache.put(serviceId, instances);
            });
            
            log.debug("Service instances cache refreshed, {} services", serviceInstancesCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh service instances cache", e);
        }
    }

    /**
     * 执行健康检查
     */
    private void performHealthChecks() {
        serviceInstancesCache.forEach((serviceId, instances) -> {
            instances.parallelStream().forEach(instance -> {
                performHealthCheck(serviceId, instance);
            });
        });
    }

    /**
     * 执行单个实例健康检查
     */
    private void performHealthCheck(String serviceId, ServiceInstance instance) {
        try {
            String healthUrl = instance.getUri() + "/actuator/health";
            Boolean isHealthy = restTemplate.getForObject(healthUrl, Boolean.class);
            
            String cacheKey = serviceId + ":" + instance.getInstanceId();
            healthStatusCache.put(cacheKey, isHealthy != null ? isHealthy : false);
            
            log.debug("Health check completed for {}: {}", cacheKey, isHealthy);
        } catch (Exception e) {
            String cacheKey = serviceId + ":" + instance.getInstanceId();
            healthStatusCache.put(cacheKey, false);
            log.debug("Health check failed for {}: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * 检查限流
     */
    public boolean checkRateLimit(String key, RouteDefinitionEntity.RateLimitConfig config) {
        if (config == null || !config.isEnabled()) {
            return true;
        }
        
        RateLimitCounter counter = rateLimitCounters.computeIfAbsent(key, k -> new RateLimitCounter());
        
        long currentTime = System.currentTimeMillis();
        counter.recordRequest(currentTime);
        
        // 检查各种限流规则
        if (config.getRequestsPerSecond() != null && 
            counter.getRequestsInLastSecond() > config.getRequestsPerSecond()) {
            statistics.incrementRateLimitedRequests();
            return false;
        }
        
        if (config.getRequestsPerMinute() != null && 
            counter.getRequestsInLastMinute() > config.getRequestsPerMinute()) {
            statistics.incrementRateLimitedRequests();
            return false;
        }
        
        if (config.getRequestsPerHour() != null && 
            counter.getRequestsInLastHour() > config.getRequestsPerHour()) {
            statistics.incrementRateLimitedRequests();
            return false;
        }
        
        return true;
    }

    /**
     * 清理限流计数器
     */
    private void cleanupRateLimitCounters() {
        long currentTime = System.currentTimeMillis();
        rateLimitCounters.entrySet().removeIf(entry -> {
            RateLimitCounter counter = entry.getValue();
            return currentTime - counter.getLastRequestTime() > TimeUnit.HOURS.toMillis(1);
        });
    }

    /**
     * 验证路由配置
     */
    private boolean validateRoute(RouteDefinitionEntity route) {
        // 基础验证
        if (route.getRouteId() == null || route.getRouteId().trim().isEmpty()) {
            return false;
        }
        
        if (route.getUri() == null) {
            return false;
        }
        
        if (route.getPredicates() == null || route.getPredicates().isEmpty()) {
            return false;
        }
        
        // 其他业务验证...
        return true;
    }

    /**
     * 获取路由统计信息
     */
    public RoutingStatistics getStatistics() {
        return statistics;
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}