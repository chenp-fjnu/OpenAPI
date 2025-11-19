package com.group.gateway.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    
    // 成功响应码
    SUCCESS(200, "操作成功"),
    
    // 客户端错误响应码
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "访问被禁止"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    
    // 服务器错误响应码
    INTERNAL_ERROR(500, "内部服务器错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    
    // 业务相关响应码
    VALIDATION_ERROR(1001, "数据校验失败"),
    DATA_NOT_EXIST(1002, "数据不存在"),
    DATA_ALREADY_EXIST(1003, "数据已存在"),
    OPERATION_FAILED(1004, "操作失败"),
    
    // 认证授权响应码
    TOKEN_INVALID(2001, "Token无效"),
    TOKEN_EXPIRED(2002, "Token已过期"),
    TOKEN_MALFORMED(2003, "Token格式错误"),
    ACCESS_DENIED(2004, "访问被拒绝"),
    PERMISSION_DENIED(2005, "权限不足"),
    
    // 网关相关响应码
    RATE_LIMIT_EXCEEDED(3001, "请求频率超限"),
    CIRCUIT_BREAKER_OPEN(3002, "服务暂时不可用"),
    ROUTE_NOT_FOUND(3003, "路由规则未找到"),
    BACKEND_SERVICE_ERROR(3004, "后端服务错误"),
    
    // 系统相关响应码
    SYSTEM_MAINTENANCE(4001, "系统维护中"),
    RESOURCE_EXCEEDED(4002, "资源使用超出限制"),
    CONCURRENT_LIMIT(4003, "并发请求超限"), 
    ERROR(400, "系统错误");
    
    /**
     * 响应码
     */
    private final Integer code;
    
    /**
     * 响应消息
     */
    private final String message;
    
    /**
     * 根据响应码获取枚举对象
     */
    public static ResultCode getByCode(Integer code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return INTERNAL_ERROR;
    }
    
    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}