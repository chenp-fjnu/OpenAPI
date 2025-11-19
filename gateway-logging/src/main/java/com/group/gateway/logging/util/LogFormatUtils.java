package com.group.gateway.logging.util;

import com.group.gateway.logging.entity.LogEntry;
import com.group.gateway.logging.config.LoggingProperties;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * 日志格式化工具类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Component
public class LogFormatUtils {
    
    // 时间格式化器
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    // 常用的格式模式
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{.*\\}|\\[.*\\]");
    private static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
    
    // 日志级别颜色映射（用于控制台输出）
    private static final String COLOR_RESET = "\u001B[0m";
    private static final String COLOR_RED = "\u001B[31m";
    private static final String COLOR_YELLOW = "\u001B[33m";
    private static final String COLOR_BLUE = "\u001B[34m";
    private static final String COLOR_GREEN = "\u001B[32m";
    private static final String COLOR_CYAN = "\u001B[36m";
    private static final String COLOR_MAGENTA = "\u001B[35m";
    
    // 配置属性
    private LoggingProperties loggingProperties;
    
    public void setLoggingProperties(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }
    
    /**
     * 格式化日志条目为字符串
     * 
     * @param logEntry 日志条目
     * @param formatType 格式类型 (SIMPLE, JSON, XML, CUSTOM)
     * @return 格式化后的字符串
     */
    public String formatLogEntry(LogEntry logEntry, String formatType) {
        switch (formatType != null ? formatType.toUpperCase() : "SIMPLE") {
            case "JSON":
                return formatAsJson(logEntry);
            case "XML":
                return formatAsXml(logEntry);
            case "CUSTOM":
                return formatAsCustom(logEntry);
            default:
                return formatAsSimple(logEntry);
        }
    }
    
    /**
     * 简单格式（默认）
     */
    public String formatAsSimple(LogEntry logEntry) {
        StringBuilder sb = new StringBuilder();
        
        // 时间戳
        String timestamp = logEntry.getTimestamp() != null ?
                logEntry.getTimestamp().format(TIMESTAMP_FORMATTER) :
                LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        sb.append(timestamp).append(" ");
        
        // 日志级别（带颜色）
        String levelWithColor = addLevelColor(logEntry.getLevel());
        sb.append(levelWithColor).append(" ");
        
        // 线程名称
        sb.append(String.format("[%-15s] ", logEntry.getThreadName()));
        
        // 记录器名称
        sb.append(String.format("%-30s - ", logEntry.getLoggerName()));
        
        // 消息内容
        sb.append(maskSensitiveInfo(logEntry.getMessage()));
        
        // 添加跟踪信息
        appendTraceInfo(sb, logEntry);
        
        // 添加HTTP信息
        appendHttpInfo(sb, logEntry);
        
        return sb.toString();
    }
    
    /**
     * JSON格式
     */
    public String formatAsJson(LogEntry logEntry) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        appendJsonField(sb, "timestamp", logEntry.getTimestamp() != null ? 
                logEntry.getTimestamp().format(TIMESTAMP_FORMATTER) : 
                LocalDateTime.now().format(TIMESTAMP_FORMATTER), true);
        appendJsonField(sb, "level", logEntry.getLevel(), true);
        appendJsonField(sb, "logger", logEntry.getLoggerName(), true);
        appendJsonField(sb, "message", maskSensitiveInfo(logEntry.getMessage()), true);
        appendJsonField(sb, "thread", logEntry.getThreadName(), true);
        
        // 可选的跟踪信息
        if (logEntry.getTraceId() != null) {
            appendJsonField(sb, "traceId", logEntry.getTraceId(), true);
        }
        if (logEntry.getSpanId() != null) {
            appendJsonField(sb, "spanId", logEntry.getSpanId(), true);
        }
        
        // HTTP信息
        if (logEntry.getHttp() != null) {
            sb.append(",\"http\":{");
            appendJsonField(sb, "method", logEntry.getHttp().getMethod(), true);
            appendJsonField(sb, "url", logEntry.getHttp().getUrl(), true);
            appendJsonField(sb, "statusCode", logEntry.getHttp().getStatusCode(), false);
            sb.append("}");
        }
        
        // 异常信息
        if (logEntry.getThrowable() != null) {
            appendJsonField(sb, "exception", formatThrowableInfo(logEntry.getThrowable()), true);
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    /**
     * XML格式
     */
    public String formatAsXml(LogEntry logEntry) {
        StringBuilder sb = new StringBuilder();
        sb.append("<log>");
        
        appendXmlField(sb, "timestamp", logEntry.getTimestamp() != null ?
                logEntry.getTimestamp().format(TIMESTAMP_FORMATTER) :
                LocalDateTime.now().format(TIMESTAMP_FORMATTER));
        appendXmlField(sb, "level", logEntry.getLevel());
        appendXmlField(sb, "logger", logEntry.getLoggerName());
        appendXmlField(sb, "message", maskSensitiveInfo(logEntry.getMessage()));
        appendXmlField(sb, "thread", logEntry.getThreadName());
        
        if (logEntry.getTraceId() != null) {
            appendXmlField(sb, "traceId", logEntry.getTraceId());
        }
        if (logEntry.getSpanId() != null) {
            appendXmlField(sb, "spanId", logEntry.getSpanId());
        }
        
        sb.append("</log>");
        return sb.toString();
    }
    
    /**
     * 自定义格式
     */
    public String formatAsCustom(LogEntry logEntry) {
        String pattern = loggingProperties != null && 
                loggingProperties.getOutput() != null && 
                loggingProperties.getOutput().getFormat() != null ?
                loggingProperties.getOutput().getFormat() : 
                "%timestamp %level [%thread] %logger - %message";
        
        String result = pattern;
        result = result.replace("%timestamp", formatTimestamp(logEntry));
        result = result.replace("%level", logEntry.getLevel());
        result = result.replace("%thread", logEntry.getThreadName());
        result = result.replace("%logger", logEntry.getLoggerName());
        result = result.replace("%message", maskSensitiveInfo(logEntry.getMessage()));
        result = result.replace("%traceId", logEntry.getTraceId() != null ? logEntry.getTraceId() : "");
        result = result.replace("%spanId", logEntry.getSpanId() != null ? logEntry.getSpanId() : "");
        
        return result;
    }
    
    /**
     * 格式化时间戳
     */
    public String formatTimestamp(LogEntry logEntry) {
        if (logEntry.getTimestamp() != null) {
            return logEntry.getTimestamp().format(TIMESTAMP_FORMATTER);
        }
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }
    
    /**
     * 添加日志级别颜色
     */
    private String addLevelColor(String level) {
        if (loggingProperties != null && 
            loggingProperties.getOutput() != null && 
            !loggingProperties.getOutput().isEnableColor()) {
            return level;
        }
        
        switch (level != null ? level.toUpperCase() : "INFO") {
            case "ERROR":
                return COLOR_RED + level + COLOR_RESET;
            case "WARN":
            case "WARNING":
                return COLOR_YELLOW + level + COLOR_RESET;
            case "INFO":
                return COLOR_GREEN + level + COLOR_RESET;
            case "DEBUG":
                return COLOR_BLUE + level + COLOR_RESET;
            case "TRACE":
                return COLOR_CYAN + level + COLOR_RESET;
            default:
                return COLOR_MAGENTA + level + COLOR_RESET;
        }
    }
    
    /**
     * 掩码敏感信息
     */
    public String maskSensitiveInfo(String message) {
        if (message == null) {
            return "";
        }
        
        String masked = message;
        
        // 掩码密码
        masked = masked.replaceAll("(?i)(password\\s*[:=]\\s*)([^\\s,;]+)", "$1***");
        masked = masked.replaceAll("(?i)(pwd\\s*[:=]\\s*)([^\\s,;]+)", "$1***");
        masked = masked.replaceAll("(?i)(\"password\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
        
        // 掩码token
        masked = masked.replaceAll("(?i)(token\\s*[:=]\\s*)([^\\s,;]+)", "$1***");
        masked = masked.replaceAll("(?i)(\"token\"\\s*:\\s*\")([^\"]+)(\")", "$1***$3");
        
        // 掩码信用卡号
        masked = masked.replaceAll("(\\d{4})(\\d{4})(\\d{4})(\\d{4})", "$1-$2-$3-$4");
        masked = masked.replaceAll("(\\d{4}-\\d{4}-\\d{4}-\\d{4})", "****-****-****-****");
        
        return masked;
    }
    
    /**
     * 清理消息内容（移除特殊字符）
     */
    public String cleanMessage(String message) {
        if (message == null) {
            return "";
        }
        
        // 移除不可见字符
        String cleaned = message.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // 处理换行符
        cleaned = NEWLINE_PATTERN.matcher(cleaned).replaceAll(" ");
        
        // 限制长度
        if (cleaned.length() > 10000) {
            cleaned = cleaned.substring(0, 10000) + "...[TRUNCATED]";
        }
        
        return cleaned.trim();
    }
    
    /**
     * 验证日志级别
     */
    public boolean isValidLevel(String level) {
        if (level == null) {
            return false;
        }
        
        String upperLevel = level.toUpperCase();
        return upperLevel.equals("TRACE") || 
               upperLevel.equals("DEBUG") || 
               upperLevel.equals("INFO") || 
               upperLevel.equals("WARN") || 
               upperLevel.equals("WARNING") || 
               upperLevel.equals("ERROR") || 
               upperLevel.equals("FATAL");
    }
    
    /**
     * 验证日志格式
     */
    public boolean isValidFormat(String format) {
        if (format == null) {
            return true; // 默认格式总是有效的
        }
        
        String upperFormat = format.toUpperCase();
        return upperFormat.equals("SIMPLE") || 
               upperFormat.equals("JSON") || 
               upperFormat.equals("XML") || 
               upperFormat.equals("CUSTOM");
    }
    
    // === 私有辅助方法 ===
    
    private void appendTraceInfo(StringBuilder sb, LogEntry logEntry) {
        if (logEntry.getTraceId() != null || logEntry.getSpanId() != null) {
            sb.append(" [");
            boolean first = true;
            if (logEntry.getTraceId() != null) {
                if (!first) sb.append(",");
                sb.append("traceId=").append(logEntry.getTraceId());
                first = false;
            }
            if (logEntry.getSpanId() != null) {
                if (!first) sb.append(",");
                sb.append("spanId=").append(logEntry.getSpanId());
            }
            sb.append("]");
        }
    }
    
    private void appendHttpInfo(StringBuilder sb, LogEntry logEntry) {
        if (logEntry.getHttp() != null) {
            sb.append(" [");
            boolean first = true;
            if (logEntry.getHttp().getMethod() != null) {
                if (!first) sb.append(",");
                sb.append(logEntry.getHttp().getMethod());
                first = false;
            }
            if (logEntry.getHttp().getUrl() != null) {
                if (!first) sb.append(" ");
                sb.append(logEntry.getHttp().getUrl());
            }
            if (logEntry.getHttp().getStatusCode() != null) {
                if (!first) sb.append(",");
                sb.append("status=").append(logEntry.getHttp().getStatusCode());
            }
            sb.append("]");
        }
    }
    
    private void appendJsonField(StringBuilder sb, String field, String value, boolean comma) {
        sb.append("\"").append(field).append("\":\"").append(value).append("\"");
        if (comma) sb.append(",");
    }
    
    private void appendJsonField(StringBuilder sb, String field, Integer value, boolean comma) {
        if (value != null) {
            sb.append("\"").append(field).append("\":").append(value);
            if (comma) sb.append(",");
        }
    }
    
    private void appendXmlField(StringBuilder sb, String field, String value) {
        sb.append("<").append(field).append(">")
          .append(escapeXml(value))
          .append("</").append(field).append(">");
    }
    
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    private String formatThrowableInfo(LogEntry.ThrowableInfo throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClassName());
        if (throwable.getMessage() != null) {
            sb.append(": ").append(throwable.getMessage());
        }
        return sb.toString();
    }
}