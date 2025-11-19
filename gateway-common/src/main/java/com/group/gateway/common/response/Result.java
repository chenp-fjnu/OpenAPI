package com.group.gateway.common.response;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 通用响应结果封装类
 * 
 * @param <T> 响应数据类型
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 时间戳
     */
    private Long timestamp;
    
    /**
     * 成功标志
     */
    private Boolean success;
    
    /**
     * 私有构造函数
     */
    private Result(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), 
                          ResultCode.SUCCESS.getMessage(), 
                          null, 
                          true);
    }
    
    /**
     * 成功响应，带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), 
                          ResultCode.SUCCESS.getMessage(), 
                          data, 
                          true);
    }
    
    /**
     * 成功响应，带消息
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), 
                          message, 
                          data, 
                          true);
    }
    
    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), 
                          ResultCode.ERROR.getMessage(), 
                          null, 
                          false);
    }
    
    /**
     * 失败响应，带消息
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), 
                          message, 
                          null, 
                          false);
    }
    
    /**
     * 失败响应，带错误码和消息
     */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, 
                          message, 
                          null, 
                          false);
    }
    
    /**
     * 自定义响应结果
     */
    public static <T> Result<T> result(Integer code, String message, T data, Boolean success) {
        return new Result<>(code, message, data, success);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(this.success);
    }
    
    /**
     * 转换为JSON字符串
     */
    public String toJson() {
        return JSON.toJSONString(this);
    }
    
    /**
     * 重写toString方法
     */
    @Override
    public String toString() {
        return toJson();
    }
}