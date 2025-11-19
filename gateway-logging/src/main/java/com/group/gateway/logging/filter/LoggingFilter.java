package com.group.gateway.logging.filter;

import com.group.gateway.logging.entity.LogEntry;
import com.group.gateway.logging.service.LoggingService;
import com.group.gateway.logging.util.LogFormatUtils;
import com.group.gateway.logging.util.LogLevelUtils;
import com.group.gateway.logging.util.LogFileWriter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 日志过滤器
 * 用于拦截HTTP请求并记录请求和响应日志
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private LogFileWriter logFileWriter;
    
    @Autowired
    private LogFormatUtils logFormatUtils;
    
    @Autowired
    private LogLevelUtils logLevelUtils;
    
    // 线程本地变量存储请求开始时间
    private static final ThreadLocal<Long> REQUEST_START_TIME = new ThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    
    // 需要记录请求体的路径模式
    private static final Set<String> LOG_REQUEST_BODY_PATHS = Set.of(
        "/api", "/v1/api", "/auth/login", "/auth/register", "/admin"
    );
    
    // 不需要记录日志的路径
    private static final Set<String> EXCLUDE_PATHS = Set.of(
        "/actuator/health", "/actuator/metrics", "/favicon.ico", "/health"
    );
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // 检查是否需要跳过日志记录
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 包装请求和响应
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        try {
            // 记录请求开始时间和跟踪ID
            REQUEST_START_TIME.set(System.currentTimeMillis());
            TRACE_ID.set(generateTraceId());
            
            // 记录请求日志
            LogEntry requestLog = createRequestLog(requestWrapper);
            logRequestAsync(requestLog);
            
            // 执行请求处理
            filterChain.doFilter(requestWrapper, responseWrapper);
            
        } finally {
            // 记录响应日志
            LogEntry responseLog = createResponseLog(requestWrapper, responseWrapper);
            logResponseAsync(responseLog);
            
            // 清理线程本地变量
            REQUEST_START_TIME.remove();
            TRACE_ID.remove();
            
            // 确保响应数据被写出
            responseWrapper.copyBodyToResponse();
        }
    }
    
    /**
     * 检查是否需要跳过日志记录
     */
    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // 检查排除路径
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.startsWith(excludePath)) {
                return true;
            }
        }
        
        // TODO: 从配置中读取排除路径列表
        return false;
    }
    
    /**
     * 创建请求日志条目
     */
    private LogEntry createRequestLog(ContentCachingRequestWrapper request) {
        LogEntry logEntry = LogEntry.builder()
                .timestamp(LocalDateTime.now())
                .level("INFO")
                .loggerName(getClass().getName())
                .threadName(Thread.currentThread().getName())
                .message(createRequestMessage(request))
                .traceId(TRACE_ID.get())
                .spanId(generateSpanId())
                .build();
        
        // 设置HTTP信息
        LogEntry.HttpInfo httpInfo = new LogEntry.HttpInfo();
        httpInfo.setMethod(request.getMethod());
        httpInfo.setUrl(request.getRequestURI());
        httpInfo.setQueryString(request.getQueryString());
        httpInfo.setUserAgent(request.getHeader("User-Agent"));
        httpInfo.setClientIp(getClientIp(request));
        
        logEntry.setHttp(httpInfo);
        
        // 如果需要记录请求体
        if (shouldLogRequestBody(request)) {
            try {
                byte[] requestBody = request.getContentAsByteArray();
                if (requestBody.length > 0) {
                    httpInfo.setRequestBody(new String(requestBody));
                }
            } catch (Exception e) {
                // 忽略请求体解析错误
            }
        }
        
        return logEntry;
    }
    
    /**
     * 创建响应日志条目
     */
    private LogEntry createResponseLog(ContentCachingRequestWrapper request, 
                                     ContentCachingResponseWrapper response) {
        LogEntry logEntry = LogEntry.builder()
                .timestamp(LocalDateTime.now())
                .level("INFO")
                .loggerName(getClass().getName())
                .threadName(Thread.currentThread().getName())
                .message(createResponseMessage(request, response))
                .traceId(TRACE_ID.get())
                .spanId(generateSpanId())
                .build();
        
        // 计算处理时间
        Long startTime = REQUEST_START_TIME.get();
        if (startTime != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            logEntry.setProcessingTime(processingTime);
        }
        
        // 设置HTTP信息
        LogEntry.HttpInfo httpInfo = new LogEntry.HttpInfo();
        httpInfo.setMethod(request.getMethod());
        httpInfo.setUrl(request.getRequestURI());
        httpInfo.setStatusCode(response.getStatus());
        httpInfo.setContentType(response.getContentType());
        
        logEntry.setHttp(httpInfo);
        
        // 如果需要记录响应体
        if (shouldLogResponseBody(request, response)) {
            try {
                byte[] responseBody = response.getContentAsByteArray();
                if (responseBody.length > 0) {
                    httpInfo.setResponseBody(new String(responseBody));
                }
            } catch (Exception e) {
                // 忽略响应体解析错误
            }
        }
        
        // 根据状态码确定日志级别
        int statusCode = response.getStatus();
        String level = determineLogLevel(statusCode);
        logEntry.setLevel(level);
        
        return logEntry;
    }
    
    /**
     * 创建请求消息
     */
    private String createRequestMessage(ContentCachingRequestWrapper request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Incoming request: ");
        sb.append(request.getMethod());
        sb.append(" ");
        sb.append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            sb.append("?");
            sb.append(queryString);
        }
        
        sb.append(" from ");
        sb.append(getClientIp(request));
        
        return sb.toString();
    }
    
    /**
     * 创建响应消息
     */
    private String createResponseMessage(ContentCachingRequestWrapper request, 
                                       ContentCachingResponseWrapper response) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request completed: ");
        sb.append(request.getMethod());
        sb.append(" ");
        sb.append(request.getRequestURI());
        sb.append(" -> ");
        sb.append(response.getStatus());
        
        Long startTime = REQUEST_START_TIME.get();
        if (startTime != null) {
            long processingTime = System.currentTimeMillis() - startTime;
            sb.append(" (");
            sb.append(processingTime);
            sb.append("ms)");
        }
        
        return sb.toString();
    }
    
    /**
     * 异步记录请求日志
     */
    private void logRequestAsync(LogEntry logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                // 记录到文件
                logFileWriter.writeLogEntry(logEntry);
                
                // 记录到服务
                loggingService.log(logEntry);
                
            } catch (Exception e) {
                // 静默处理日志记录错误，避免影响主业务流程
                System.err.println("Failed to log request: " + e.getMessage());
            }
        });
    }
    
    /**
     * 异步记录响应日志
     */
    private void logResponseAsync(LogEntry logEntry) {
        CompletableFuture.runAsync(() -> {
            try {
                // 记录到文件
                logFileWriter.writeLogEntry(logEntry);
                
                // 记录到服务
                loggingService.log(logEntry);
                
            } catch (Exception e) {
                // 静默处理日志记录错误，避免影响主业务流程
                System.err.println("Failed to log response: " + e.getMessage());
            }
        });
    }
    
    /**
     * 检查是否需要记录请求体
     */
    private boolean shouldLogRequestBody(ContentCachingRequestWrapper request) {
        String path = request.getRequestURI();
        
        // 检查路径是否在需要记录的列表中
        for (String logPath : LOG_REQUEST_BODY_PATHS) {
            if (path.startsWith(logPath)) {
                return true;
            }
        }
        
        // TODO: 从配置中读取需要记录请求体的路径列表
        return false;
    }
    
    /**
     * 检查是否需要记录响应体
     */
    private boolean shouldLogResponseBody(ContentCachingRequestWrapper request, 
                                        ContentCachingResponseWrapper response) {
        // 只记录错误响应
        int statusCode = response.getStatus();
        return statusCode >= 400;
    }
    
    /**
     * 根据HTTP状态码确定日志级别
     */
    private String determineLogLevel(int statusCode) {
        if (statusCode >= 500) {
            return "ERROR";
        } else if (statusCode >= 400) {
            return "WARN";
        } else {
            return "INFO";
        }
    }
    
    /**
     * 生成跟踪ID
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * 生成跨度ID
     */
    private String generateSpanId() {
        return Long.toHexString(System.nanoTime());
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取跟踪ID
     */
    public static String getCurrentTraceId() {
        return TRACE_ID.get();
    }
    
    /**
     * 获取请求开始时间
     */
    public static Long getRequestStartTime() {
        return REQUEST_START_TIME.get();
    }
}