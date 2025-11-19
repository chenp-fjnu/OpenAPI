package com.group.gateway.core.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 客户端信息
 * 用于记录请求客户端的相关信息，包括IP、用户代理、设备类型等
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {
    
    /**
     * 客户端IP地址
     */
    private String clientIp;
    
    /**
     * 客户端端口
     */
    private Integer clientPort;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 客户端类型
     */
    private ClientType clientType;
    
    /**
     * 设备类型
     */
    private DeviceType deviceType;
    
    /**
     * 操作系统
     */
    private String operatingSystem;
    
    /**
     * 浏览器名称
     */
    private String browserName;
    
    /**
     * 浏览器版本
     */
    private String browserVersion;
    
    /**
     * 设备品牌
     */
    private String deviceBrand;
    
    /**
     * 设备型号
     */
    private String deviceModel;
    
    /**
     * 网络类型
     */
    private NetworkType networkType;
    
    /**
     * 地理位置信息
     */
    private LocationInfo locationInfo;
    
    /**
     * 是否为移动设备
     */
    private boolean isMobile;
    
    /**
     * 是否为爬虫
     */
    private boolean isBot;
    
    /**
     * API客户端ID
     */
    private String clientId;
    
    /**
     * 应用版本
     */
    private String appVersion;
    
    /**
     * 获取客户端标识
     */
    public String getClientIdentifier() {
        if (clientId != null && !clientId.isEmpty()) {
            return "client:" + clientId;
        }
        return "ip:" + clientIp;
    }
    
    /**
     * 获取设备描述
     */
    public String getDeviceDescription() {
        StringBuilder desc = new StringBuilder();
        
        if (operatingSystem != null) {
            desc.append(operatingSystem);
        }
        
        if (browserName != null) {
            if (desc.length() > 0) desc.append(" ");
            desc.append(browserName);
        }
        
        if (browserVersion != null) {
            desc.append(" ").append(browserVersion);
        }
        
        return desc.toString();
    }
    
    /**
     * 获取简化的设备类型
     */
    public String getSimplifiedDeviceType() {
        if (isBot) {
            return "bot";
        } else if (isMobile) {
            return "mobile";
        } else if (clientType == ClientType.API) {
            return "api";
        } else {
            return "desktop";
        }
    }
    
    /**
     * 检查是否为可信客户端
     */
    public boolean isTrustedClient() {
        // 这里可以根据业务逻辑判断是否为可信客户端
        // 例如：API客户端、具有特定IP段的内部服务等
        return clientType == ClientType.API || 
               clientType == ClientType.INTERNAL ||
               (clientId != null && !clientId.isEmpty());
    }
    
    /**
     * 客户端类型枚举
     */
    public enum ClientType {
        WEB_BROWSER,    // 网页浏览器
        MOBILE_APP,     // 移动应用
        API_CLIENT,     // API客户端
        SYSTEM_SERVICE, // 系统服务
        INTERNAL,       // 内部服务
        UNKNOWN         // 未知
    }
    
    /**
     * 设备类型枚举
     */
    public enum DeviceType {
        DESKTOP,    // 桌面设备
        MOBILE,     // 移动设备
        TABLET,     // 平板设备
        TV,         // 电视设备
        IOT         // IoT设备
    }
    
    /**
     * 网络类型枚举
     */
    public enum NetworkType {
        WIFI,       // WiFi
        CELLULAR,   // 蜂窝网络
        ETHERNET,   // 以太网
        UNKNOWN     // 未知
    }
    
    @Override
    public String toString() {
        return "ClientInfo{" +
                "clientIp='" + clientIp + '\'' +
                ", userAgent='" + (userAgent != null ? 
                    (userAgent.length() > 50 ? userAgent.substring(0, 50) + "..." : userAgent) : "null") + '\'' +
                ", clientType=" + clientType +
                ", deviceType=" + deviceType +
                ", isMobile=" + isMobile +
                ", isBot=" + isBot +
                ", clientId='" + clientId + '\'' +
                '}';
    }
}