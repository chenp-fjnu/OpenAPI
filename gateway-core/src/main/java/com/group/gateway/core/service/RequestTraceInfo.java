package com.group.gateway.core.service;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * 请求链路追踪信息
 * 用于记录单个请求的完整生命周期信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestTraceInfo {
    
    /**
     * 链路追踪ID
     */
    private String traceId;
    
    /**
     * 请求开始时间
     */
    private Instant requestTime;
    
    /**
     * 请求结束时间
     */
    private Instant endTime;
    
    /**
     * HTTP方法
     */
    private String method;
    
    /**
     * 请求路径
     */
    private String path;
    
    /**
     * 查询参数
     */
    private String query;
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 租户ID
     */
    private String tenantId;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * HTTP状态码
     */
    private int statusCode;
    
    /**
     * 请求执行时长（毫秒）
     */
    private long duration;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误类型
     */
    private String errorClass;
    
    /**
     * 业务模块（用于业务分类统计）
     */
    private String businessModule;
    
    /**
     * 服务实例ID
     */
    private String serviceInstanceId;
    
    /**
     * 获取请求时长（分钟级）
     */
    public long getDurationMinutes() {
        return duration / (1000 * 60);
    }
    
    /**
     * 获取请求时长（秒级）
     */
    public long getDurationSeconds() {
        return duration / 1000;
    }
    
    /**
     * 获取请求时长的可读字符串
     */
    public String getDurationString() {
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
        return duration > 5000; // 超过5秒视为慢请求
    }
    
    /**
     * 检查是否为正常响应状态码
     */
    public boolean isSuccessfulStatusCode() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        if (success && isSuccessfulStatusCode()) {
            return "成功";
        } else if (success) {
            return "业务异常";
        } else {
            return "系统异常";
        }
    }
    
    /**
     * 获取客户端标识（用户ID或客户端ID）
     */
    public String getClientIdentifier() {
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        } else if (clientId != null && !clientId.isEmpty()) {
            return "client:" + clientId;
        } else {
            return "unknown";
        }
    }
    
    /**
     * 获取租户标识
     */
    public String getTenantIdentifier() {
        return tenantId != null && !tenantId.isEmpty() ? tenantId : "default";
    }
    
    /**
     * 生成简洁的追踪摘要信息
     */
    public String getTraceSummary() {
        return String.format(
            "traceId=%s | %s %s | %d | %s | %s | %s",
            traceId,
            method,
            path,
            statusCode,
            getDurationString(),
            getStatusDescription(),
            getClientIdentifier()
        );
    }
    
    @Override
    public String toString() {
        return "RequestTraceInfo{" +
                "traceId='" + traceId + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", statusCode=" + statusCode +
                ", duration=" + duration +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", clientIdentifier='" + getClientIdentifier() + '\'' +
                '}';
    }
}