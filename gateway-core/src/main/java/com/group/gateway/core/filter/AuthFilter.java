package com.group.gateway.core.filter;

import com.group.gateway.common.response.Result;
import com.group.gateway.common.response.ResultCode;
import com.group.gateway.core.service.AuthService;
import com.group.gateway.core.service.RequestContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 认证授权过滤器
 * 负责验证用户身份、权限校验和JWT令牌处理
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    // 白名单路径，不需要认证
    private static final List<String> WHITE_LIST_PATHS = Arrays.asList(
        "/api/auth/login",
        "/api/auth/register", 
        "/api/auth/refresh",
        "/api/public/",
        "/api/health",
        "/actuator/health",
        "/swagger-ui/",
        "/swagger-resources/",
        "/v3/api-docs/"
    );
    
    // 管理API路径前缀
    private static final String ADMIN_API_PREFIX = "/api/admin/";
    
    private final AuthService authService;
    private final RequestContextService requestContextService;
    
    @Override
    public int getOrder() {
        // 在请求日志过滤器之后，限流过滤器之前
        return 100;
    }
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod() != null ? request.getMethod().name() : "GET";
        
        // 1. 检查是否在白名单中
        if (isWhitelistedPath(path)) {
            log.debug("路径在白名单中，跳过认证 | path={}", path);
            return chain.filter(exchange);
        }
        
        // 2. 获取认证信息
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        String userId = request.getHeaders().getFirst("X-User-ID");
        String token = extractToken(authHeader);
        
        // 3. 验证认证信息
        return authService.validateAuth(exchange, token, userId)
            .flatMap(authResult -> {
                if (authResult.isSuccess()) {
                    // 认证成功，设置上下文信息
                    setRequestContext(exchange, authResult);
                    
                    // 检查是否为管理API，需要管理员权限
                    if (isAdminApi(path) && !authResult.isAdmin()) {
                        log.warn("非管理员访问管理API | userId={} | path={}", 
                            authResult.getUserId(), path);
                        return unauthorizedResponse(exchange, "需要管理员权限");
                    }
                    
                    return chain.filter(exchange);
                } else {
                    // 认证失败
                    log.warn("认证失败 | path={} | method={} | reason={}", 
                        path, method, authResult.getMessage());
                    return unauthorizedResponse(exchange, authResult.getMessage());
                }
            })
            .onErrorResume(error -> {
                log.error("认证过程中发生错误 | path={} | error={}", path, error.getMessage(), error);
                return unauthorizedResponse(exchange, "认证服务异常");
            });
    }
    
    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelistedPath(String path) {
        return WHITE_LIST_PATHS.stream().anyMatch(path::startsWith);
    }
    
    /**
     * 检查是否为管理API
     */
    private boolean isAdminApi(String path) {
        return path.startsWith(ADMIN_API_PREFIX);
    }
    
    /**
     * 从Authorization头中提取令牌
     */
    private String extractToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return null;
        }
        
        if (authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        // 也支持Basic认证
        if (authHeader.startsWith("Basic ")) {
            return authHeader.substring("Basic ".length());
        }
        
        return authHeader;
    }
    
    /**
     * 设置请求上下文信息
     */
    private void setRequestContext(ServerWebExchange exchange, AuthResult authResult) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        
        // 设置用户信息到请求头，供下游服务使用
        if (StringUtils.hasText(authResult.getUserId())) {
            builder.header("X-User-ID", authResult.getUserId());
        }
        if (StringUtils.hasText(authResult.getTenantId())) {
            builder.header("X-Tenant-ID", authResult.getTenantId());
        }
        if (StringUtils.hasText(authResult.getRoles())) {
            builder.header("X-User-Roles", authResult.getRoles());
        }
        if (StringUtils.hasText(authResult.getClientId())) {
            builder.header("X-Client-ID", authResult.getClientId());
        }
        
        // 添加内部追踪标识
        builder.header("X-Internal-Trace-ID", authResult.getTraceId());
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(builder.build())
            .build();
        
        // 设置到上下文服务
        requestContextService.setAuthInfo(mutatedExchange, authResult);
        
        log.debug("设置请求上下文 | userId={} | tenantId={} | roles={}",
            authResult.getUserId(), authResult.getTenantId(), authResult.getRoles());
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        Result<Object> result = Result.error(ResultCode.UNAUTHORIZED, message);
        String responseBody = com.alibaba.fastjson2.JSON.toJSONString(result);
        
        return response.writeWith(
            Mono.just(responseBody.getBytes(StandardCharsets.UTF_8))
                .map(data -> response.bufferFactory().wrap(data))
        );
    }
    
    /**
     * 认证结果
     */
    public static class AuthResult {
        private boolean success;
        private String message;
        private String userId;
        private String tenantId;
        private String roles;
        private String clientId;
        private String traceId;
        private boolean admin;
        
        public static AuthResult success(String userId, String tenantId, String roles, String clientId, String traceId) {
            AuthResult result = new AuthResult();
            result.success = true;
            result.userId = userId;
            result.tenantId = tenantId;
            result.roles = roles;
            result.clientId = clientId;
            result.traceId = traceId;
            result.admin = isAdmin(roles);
            return result;
        }
        
        public static AuthResult failure(String message) {
            AuthResult result = new AuthResult();
            result.success = false;
            result.message = message;
            return result;
        }
        
        private static boolean isAdmin(String roles) {
            if (!StringUtils.hasText(roles)) {
                return false;
            }
            
            return Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getUserId() { return userId; }
        public String getTenantId() { return tenantId; }
        public String getRoles() { return roles; }
        public String getClientId() { return clientId; }
        public String getTraceId() { return traceId; }
        public boolean isAdmin() { return admin; }
    }
}