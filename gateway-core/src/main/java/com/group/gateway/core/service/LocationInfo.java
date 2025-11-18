package com.group.gateway.core.service;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 地理位置信息
 * 用于记录客户端的地理位置相关信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationInfo {
    
    /**
     * 国家
     */
    private String country;
    
    /**
     * 地区/省份
     */
    private String region;
    
    /**
     * 城市
     */
    private String city;
    
    /**
     * 经度
     */
    private Double longitude;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 时区
     */
    private String timezone;
    
    /**
     * 邮政编码
     */
    private String postalCode;
    
    /**
     * ISP提供商
     */
    private String isp;
    
    /**
     * 组织
     */
    private String organization;
    
    /**
     * AS号码
     */
    private String asNumber;
    
    /**
     * AS组织
     */
    private String asOrganization;
    
    /**
     * 连接速度
     */
    private ConnectionSpeed connectionSpeed;
    
    /**
     * 是否为数据中心
     */
    private boolean isDatacenter;
    
    /**
     * 是否为代理
     */
    private boolean isProxy;
    
    /**
     * 是否为Tor节点
     */
    private boolean isTor;
    
    /**
     * 获取完整地址描述
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (city != null && !city.isEmpty()) {
            address.append(city);
        }
        
        if (region != null && !region.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(region);
        }
        
        if (country != null && !country.isEmpty()) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }
        
        return address.toString();
    }
    
    /**
     * 检查是否为高风险地区
     */
    public boolean isHighRiskLocation() {
        // 这里可以根据实际业务需求定义高风险地区
        // 例如：某些已知的不安全地区、频繁攻击的来源地等
        return isProxy || isTor || isDatacenter;
    }
    
    /**
     * 获取地理位置标识
     */
    public String getLocationKey() {
        if (country != null && !country.isEmpty()) {
            return country + (region != null ? "_" + region : "");
        }
        return "unknown";
    }
    
    /**
     * 连接速度枚举
     */
    public enum ConnectionSpeed {
        DIALUP,      // 拨号
        DSL,         // DSL
        CABLE,       // 有线
        FIBER,       // 光纤
        SATELLITE,   // 卫星
        MOBILE,      // 移动
        UNKNOWN      // 未知
    }
    
    @Override
    public String toString() {
        return "LocationInfo{" +
                "country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", city='" + city + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", isProxy=" + isProxy +
                ", isTor=" + isTor +
                ", isDatacenter=" + isDatacenter +
                '}';
    }
}