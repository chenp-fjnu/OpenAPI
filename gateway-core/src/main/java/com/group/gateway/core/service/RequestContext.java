package com.group.gateway.core.service;

import com.group.gateway.core.filter.AuthFilter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * 请求上下文信息
 * 用于存储单个请求的完整上下文信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestContext {
    
    /**
     * 链路追踪ID
     */
    private String traceId;
    
    /**
     * 认证信息
     */
    private AuthFilter.AuthResult authInfo;
    
    /**
     * 客户端信息
     */
    private ClientInfo clientInfo;
    
    /**
     * 限流信息
     */
    private RateLimitInfo rateLimitInfo;
    
    /**
     * 请求开始时间
     */
    private Instant startTime;
    
    /**
     * 请求结束时间
     */
    private Instant endTime;
    
    /**
     * 执行时长（毫秒）
     */
    private Long duration;
    
    /**
     * 请求处理状态
     */
    private ProcessingStatus status;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 业务模块
     */
    private String businessModule;
    
    /**
     * 目标服务
     */
    private String targetService;
    
    /**
     * 目标服务URL
     */
    private String targetUrl;
    
    public RequestContext(String traceId) {
        this.traceId = traceId;
        this.status = ProcessingStatus.PENDING;
    }
    
    /**
     * 标记请求完成
     */
    public void markComplete() {
        this.endTime = Instant.now();
        this.status = ProcessingStatus.COMPLETED;
        
        if (this.startTime != null) {
            this.duration = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }
    
    /**
     * 标记请求失败
     */
    public void markFailed(String errorMessage) {
        this.endTime = Instant.now();
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        
        if (this.startTime != null) {
            this.duration = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }
    
    /**
     * 获取执行时长字符串
     */
    public String getDurationString() {
        if (duration == null) {
            return "N/A";
        }
        
        if (duration < 1000) {
            return duration + "ms";
        } else if (duration < 60 * 1000) {
            return String.format("%.1fs", duration / 1000.0);
        } else {
            return String.format("%.1fm", duration / (1000.0 * 60));
        }
    }
    
    /**
     * 检查是否为慢请求
     */
    public boolean isSlowRequest() {
        return duration != null && duration > 5000; // 超过5秒
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return authInfo != null ? authInfo.getUserId() : null;
    }
    
    /**
     * 获取租户ID
     */
    public String getTenantId() {
        return authInfo != null ? authInfo.getTenantId() : null;
    }
    
    /**
     * 获取用户角色
     */
    public String getUserRoles() {
        return authInfo != null ? authInfo.getRoles() : null;
    }
    
    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return authInfo != null && authInfo.isAdmin();
    }
    
    /**
     * 获取客户端IP
     */
    public String getClientIp() {
        return clientInfo != null ? clientInfo.getClientIp() : null;
    }
    
    /**
     * 获取用户代理
     */
    public String getUserAgent() {
        return clientInfo != null ? clientInfo.getUserAgent() : null;
    }
    
    /**
     * 生成摘要信息
     */
    public String getSummary() {
        return String.format(
            "traceId=%s | user=%s | tenant=%s | service=%s | status=%s | duration=%s",
            traceId,
            getUserId() != null ? getUserId() : "anonymous",
            getTenantId() != null ? getTenantId() : "default",
            targetService != null ? targetService : "unknown",
            status != null ? status : "unknown",
            getDurationString()
        );
    }
    
    /**
     * 处理状态枚举
     */
    public enum ProcessingStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        COMPLETED,  // 完成
        FAILED,     // 失败
        TIMEOUT     // 超时
    }
    
    @Override
    public String toString() {
        return "RequestContext{" +
                "traceId='" + traceId + '\'' +
                ", userId='" + getUserId() + '\'' +
                ", tenantId='" + getTenantId() + '\'' +
                ", status=" + status +
                ", duration=" + getDurationString() +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}