package com.group.gateway.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Gateway配置类
 * 管理网关的核心配置参数
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "gateway.config")
public class GatewayConfig {
    
    private boolean enabled = true;
    private String name = "Group API Gateway";
    private String version = "1.0.0";
    private int port = 8080;
    private String contextPath = "/";
    private boolean enableCors = true;
    private boolean enableRateLimit = true;
    private boolean enableAuth = true;
    private boolean enableLogging = true;
    private boolean enableMetrics = true;
    private boolean enableTracing = true;
    
    // CORS配置
    private Cors cors = new Cors();
    
    // 限流配置
    private RateLimit rateLimit = new RateLimit();
    
    // 认证配置
    private Auth auth = new Auth();
    
    // 负载均衡配置
    private LoadBalancer loadBalancer = new LoadBalancer();
    
    // 重试配置
    private Retry retry = new Retry();
    
    // 超时配置
    private Timeout timeout = new Timeout();
    
    // 健康检查配置
    private HealthCheck healthCheck = new HealthCheck();
    
    // 熔断器配置
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public boolean isEnableCors() {
        return enableCors;
    }
    
    public void setEnableCors(boolean enableCors) {
        this.enableCors = enableCors;
    }
    
    public boolean isEnableRateLimit() {
        return enableRateLimit;
    }
    
    public void setEnableRateLimit(boolean enableRateLimit) {
        this.enableRateLimit = enableRateLimit;
    }
    
    public boolean isEnableAuth() {
        return enableAuth;
    }
    
    public void setEnableAuth(boolean enableAuth) {
        this.enableAuth = enableAuth;
    }
    
    public boolean isEnableLogging() {
        return enableLogging;
    }
    
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }
    
    public boolean isEnableMetrics() {
        return enableMetrics;
    }
    
    public void setEnableMetrics(boolean enableMetrics) {
        this.enableMetrics = enableMetrics;
    }
    
    public boolean isEnableTracing() {
        return enableTracing;
    }
    
    public void setEnableTracing(boolean enableTracing) {
        this.enableTracing = enableTracing;
    }
    
    public Cors getCors() {
        return cors;
    }
    
    public void setCors(Cors cors) {
        this.cors = cors;
    }
    
    public RateLimit getRateLimit() {
        return rateLimit;
    }
    
    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
    
    public Auth getAuth() {
        return auth;
    }
    
    public void setAuth(Auth auth) {
        this.auth = auth;
    }
    
    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }
    
    public void setLoadBalancer(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }
    
    public Retry getRetry() {
        return retry;
    }
    
    public void setRetry(Retry retry) {
        this.retry = retry;
    }
    
    public Timeout getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }
    
    public HealthCheck getHealthCheck() {
        return healthCheck;
    }
    
    public void setHealthCheck(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }
    
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
    
    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
    
    // 内部配置类
    public static class Cors {
        private List<String> allowedOrigins = List.of("*");
        private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        private List<String> allowedHeaders = List.of("*");
        private List<String> exposedHeaders = List.of();
        private boolean allowCredentials = false;
        private long maxAge = 3600;
        
        // getters and setters
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
        public List<String> getAllowedMethods() { return allowedMethods; }
        public void setAllowedMethods(List<String> allowedMethods) { this.allowedMethods = allowedMethods; }
        public List<String> getAllowedHeaders() { return allowedHeaders; }
        public void setAllowedHeaders(List<String> allowedHeaders) { this.allowedHeaders = allowedHeaders; }
        public List<String> getExposedHeaders() { return exposedHeaders; }
        public void setExposedHeaders(List<String> exposedHeaders) { this.exposedHeaders = exposedHeaders; }
        public boolean isAllowCredentials() { return allowCredentials; }
        public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
        public long getMaxAge() { return maxAge; }
        public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    }
    
    public static class RateLimit {
        private int defaultLimit = 100;
        private int defaultWindowSeconds = 60;
        private boolean enabled = true;
        private Map<String, Integer> customLimits = Map.of();
        
        public int getDefaultLimit() { return defaultLimit; }
        public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
        public int getDefaultWindowSeconds() { return defaultWindowSeconds; }
        public void setDefaultWindowSeconds(int defaultWindowSeconds) { this.defaultWindowSeconds = defaultWindowSeconds; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public Map<String, Integer> getCustomLimits() { return customLimits; }
        public void setCustomLimits(Map<String, Integer> customLimits) { this.customLimits = customLimits; }
    }
    
    public static class Auth {
        private boolean enabled = true;
        private String jwtSecret = "default-secret";
        private long jwtExpirationMs = 86400000; // 24小时
        private boolean requireAuth = false;
        private List<String> publicPaths = List.of("/actuator/health", "/actuator/info");
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getJwtSecret() { return jwtSecret; }
        public void setJwtSecret(String jwtSecret) { this.jwtSecret = jwtSecret; }
        public long getJwtExpirationMs() { return jwtExpirationMs; }
        public void setJwtExpirationMs(long jwtExpirationMs) { this.jwtExpirationMs = jwtExpirationMs; }
        public boolean isRequireAuth() { return requireAuth; }
        public void setRequireAuth(boolean requireAuth) { this.requireAuth = requireAuth; }
        public List<String> getPublicPaths() { return publicPaths; }
        public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }
    }
    
    public static class LoadBalancer {
        private String algorithm = "round-robin";
        private int maxRetries = 3;
        private long retryDelayMs = 1000;
        
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public long getRetryDelayMs() { return retryDelayMs; }
        public void setRetryDelayMs(long retryDelayMs) { this.retryDelayMs = retryDelayMs; }
    }
    
    public static class Retry {
        private int maxAttempts = 3;
        private List<Integer> retryableStatusCodes = List.of(502, 503, 504);
        private long backoffDelayMs = 1000;
        private double backoffMultiplier = 2.0;
        
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public List<Integer> getRetryableStatusCodes() { return retryableStatusCodes; }
        public void setRetryableStatusCodes(List<Integer> retryableStatusCodes) { this.retryableStatusCodes = retryableStatusCodes; }
        public long getBackoffDelayMs() { return backoffDelayMs; }
        public void setBackoffDelayMs(long backoffDelayMs) { this.backoffDelayMs = backoffDelayMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    }
    
    public static class Timeout {
        private long connectTimeoutMs = 5000;
        private long readTimeoutMs = 30000;
        private long writeTimeoutMs = 30000;
        
        public long getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(long connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public long getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(long readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
        public long getWriteTimeoutMs() { return writeTimeoutMs; }
        public void setWriteTimeoutMs(long writeTimeoutMs) { this.writeTimeoutMs = writeTimeoutMs; }
    }
    
    public static class HealthCheck {
        private boolean enabled = true;
        private int intervalSeconds = 30;
        private int timeoutMs = 5000;
        private int maxFailures = 3;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getIntervalSeconds() { return intervalSeconds; }
        public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
        public int getMaxFailures() { return maxFailures; }
        public void setMaxFailures(int maxFailures) { this.maxFailures = maxFailures; }
    }
    
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private int successThreshold = 3;
        private long timeoutMs = 60000;
        private long resetTimeoutMs = 60000;
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public int getSuccessThreshold() { return successThreshold; }
        public void setSuccessThreshold(int successThreshold) { this.successThreshold = successThreshold; }
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }
        public long getResetTimeoutMs() { return resetTimeoutMs; }
        public void setResetTimeoutMs(long resetTimeoutMs) { this.resetTimeoutMs = resetTimeoutMs; }
    }
}