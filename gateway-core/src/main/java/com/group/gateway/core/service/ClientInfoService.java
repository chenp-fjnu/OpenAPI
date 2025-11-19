package com.group.gateway.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 客户端信息服务
 * 负责识别和分析客户端信息
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class ClientInfoService {
    
    @Autowired
    private RequestContextService requestContextService;
    
    /**
     * 设备类型识别
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(?i)(android|iphone|ipad|phone|mobile)");
    private static final Pattern TABLET_PATTERN = Pattern.compile("(?i)(ipad|tablet|kindle)");
    private static final Pattern BOT_PATTERN = Pattern.compile("(?i)(bot|crawler|spider|crawl)");
    
    /**
     * 可信IP列表
     */
    private final List<String> trustedIpRanges = Arrays.asList(
        "127.0.0.1",
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16"
    );
    
    /**
     * 缓存客户端信息
     */
    private final Map<String, ClientInfo> clientInfoCache = new ConcurrentHashMap<>();
    
    /**
     * 获取客户端信息
     */
    public Mono<ClientInfo> getClientInfo(ServerWebExchange exchange) {
        String traceId = requestContextService.getTraceId(exchange);
        String clientIp = getRealClientIp(exchange);
        
        return Mono.defer(() -> {
            try {
                log.debug("获取客户端信息 - traceId: {}, clientIp: {}", traceId, clientIp);
                
                String cacheKey = clientIp + ":" + getUserAgent(exchange);
                
                // 检查缓存
                if (clientInfoCache.containsKey(cacheKey)) {
                    return Mono.just(clientInfoCache.get(cacheKey));
                }
                
                // 创建新的客户端信息
                ClientInfo clientInfo = ClientInfo.builder()
                    .ip(clientIp)
                    .userAgent(getUserAgent(exchange))
                    .deviceType(getDeviceType(getUserAgent(exchange)))
                    .clientType(getClientType(getUserAgent(exchange)))
                    .networkType(getNetworkType(exchange))
                    .isTrusted(isTrustedIp(clientIp))
                    .acceptLanguage(getAcceptLanguage(exchange))
                    .connection(getConnectionInfo(exchange))
                    .build();
                
                // 缓存客户端信息
                clientInfoCache.put(cacheKey, clientInfo);
                
                return Mono.just(clientInfo);
                
            } catch (Exception e) {
                log.error("获取客户端信息异常 - traceId: {}, clientIp: {}", traceId, clientIp, e);
                return Mono.just(createDefaultClientInfo(clientIp));
            }
        });
    }
    
    /**
     * 获取真实的客户端IP
     */
    public String getRealClientIp(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 1. 检查X-Forwarded-For头部
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            String[] ips = forwardedFor.split(",");
            String firstIp = ips[0].trim();
            if (isValidIp(firstIp)) {
                return firstIp;
            }
        }
        
        // 2. 检查X-Real-IP头部
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && isValidIp(realIp)) {
            return realIp;
        }
        
        // 3. 检查CF-Connecting-IP（Cloudflare）
        String cfConnectingIp = request.getHeaders().getFirst("CF-Connecting-IP");
        if (cfConnectingIp != null && isValidIp(cfConnectingIp)) {
            return cfConnectingIp;
        }
        
        // 4. 检查X-Client-IP头部
        String clientIp = request.getHeaders().getFirst("X-Client-IP");
        if (clientIp != null && isValidIp(clientIp)) {
            return clientIp;
        }
        
        // 5. 从连接地址获取
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            InetAddress address = remoteAddress.getAddress();
            if (address != null) {
                return address.getHostAddress();
            }
        }
        
        return "unknown";
    }
    
    /**
     * 获取用户代理
     */
    private String getUserAgent(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("User-Agent");
    }
    
    /**
     * 获取Accept-Language
     */
    private String getAcceptLanguage(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("Accept-Language");
    }
    
    /**
     * 获取连接信息
     */
    private String getConnectionInfo(ServerWebExchange exchange) {
        StringBuilder info = new StringBuilder();
        
        String upgrade = exchange.getRequest().getHeaders().getFirst("Upgrade");
        if (upgrade != null) {
            info.append("upgrade=").append(upgrade).append(";");
        }
        
        String connection = exchange.getRequest().getHeaders().getFirst("Connection");
        if (connection != null) {
            info.append("connection=").append(connection).append(";");
        }
        
        String protocol = exchange.getRequest().getURI().getScheme();
        if (protocol != null) {
            info.append("protocol=").append(protocol).append(";");
        }
        
        return info.toString();
    }
    
    /**
     * 识别设备类型
     */
    private ClientInfo.DeviceType getDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return ClientInfo.DeviceType.UNKNOWN;
        }
        
        if (MOBILE_PATTERN.matcher(userAgent).find()) {
            if (TABLET_PATTERN.matcher(userAgent).find()) {
                return ClientInfo.DeviceType.TABLET;
            }
            return ClientInfo.DeviceType.MOBILE;
        }
        
        return ClientInfo.DeviceType.DESKTOP;
    }
    
    /**
     * 识别客户端类型
     */
    private ClientInfo.ClientType getClientType(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return ClientInfo.ClientType.UNKNOWN;
        }
        
        if (BOT_PATTERN.matcher(userAgent).find()) {
            return ClientInfo.ClientType.BOT;
        }
        
        return ClientInfo.ClientType.BROWSER;
    }
    
    /**
     * 获取网络类型
     */
    private ClientInfo.NetworkType getNetworkType(ServerWebExchange exchange) {
        String connection = exchange.getRequest().getHeaders().getFirst("Connection");
        String upgrade = exchange.getRequest().getHeaders().getFirst("Upgrade");
        
        if ("upgrade".equalsIgnoreCase(connection) && "websocket".equalsIgnoreCase(upgrade)) {
            return ClientInfo.NetworkType.WEBSOCKET;
        }
        
        return ClientInfo.NetworkType.HTTP;
    }
    
    /**
     * 验证IP地址
     */
    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 检查是否受信任的IP
     */
    private boolean isTrustedIp(String ip) {
        for (String trustedRange : trustedIpRanges) {
            if (trustedRange.contains("/")) {
                // CIDR格式
                if (isIpInCidrRange(ip, trustedRange)) {
                    return true;
                }
            } else if (trustedRange.equals(ip)) {
                // 精确匹配
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查IP是否在CIDR范围内
     */
    private boolean isIpInCidrRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String baseIp = parts[0];
            int mask = Integer.parseInt(parts[1]);
            
            String[] ipParts = ip.split("\\.");
            String[] baseParts = baseIp.split("\\.");
            
            int[] ipNums = new int[4];
            int[] baseNums = new int[4];
            
            for (int i = 0; i < 4; i++) {
                ipNums[i] = Integer.parseInt(ipParts[i]);
                baseNums[i] = Integer.parseInt(baseParts[i]);
            }
            
            int maskBits = (mask / 8);
            int maskRemainder = mask % 8;
            
            // 比较前面的字节
            for (int i = 0; i < maskBits; i++) {
                if (ipNums[i] != baseNums[i]) {
                    return false;
                }
            }
            
            // 如果还有剩余的位需要比较
            if (maskRemainder > 0 && maskBits < 4) {
                int shiftBits = 8 - maskRemainder;
                int maskValue = (0xFF << shiftBits) & 0xFF;
                return (ipNums[maskBits] & maskValue) == (baseNums[maskBits] & maskValue);
            }
            
            return true;
            
        } catch (Exception e) {
            log.warn("检查CIDR范围失败: {}", cidr, e);
            return false;
        }
    }
    
    /**
     * 创建默认客户端信息
     */
    private ClientInfo createDefaultClientInfo(String clientIp) {
        return ClientInfo.builder()
            .ip(clientIp)
            .userAgent("unknown")
            .deviceType(ClientInfo.DeviceType.UNKNOWN)
            .clientType(ClientInfo.ClientType.UNKNOWN)
            .networkType(ClientInfo.NetworkType.UNKNOWN)
            .isTrusted(isTrustedIp(clientIp))
            .acceptLanguage("unknown")
            .connection("unknown")
            .build();
    }
    
    /**
     * 获取客户端标识
     */
    public String getClientIdentifier(ClientInfo clientInfo) {
        StringBuilder identifier = new StringBuilder();
        identifier.append("ip:").append(clientInfo.getIp());
        if (clientInfo.getUserAgent() != null) {
            identifier.append(";ua:").append(clientInfo.getUserAgent().hashCode());
        }
        return identifier.toString();
    }
    
    /**
     * 获取设备描述
     */
    public String getDeviceDescription(ClientInfo clientInfo) {
        StringBuilder desc = new StringBuilder();
        desc.append(clientInfo.getDeviceType().name());
        desc.append("-").append(clientInfo.getClientType().name());
        
        if (clientInfo.getNetworkType() != ClientInfo.NetworkType.UNKNOWN) {
            desc.append("-").append(clientInfo.getNetworkType().name());
        }
        
        return desc.toString();
    }
    
    /**
     * 检查客户端类型
     */
    public boolean isBot(ClientInfo clientInfo) {
        return clientInfo.getClientType() == ClientInfo.ClientType.BOT;
    }
    
    /**
     * 检查可信客户端
     */
    public boolean isTrustedClient(ClientInfo clientInfo) {
        return clientInfo.isTrusted() || isBot(clientInfo);
    }
    
    /**
     * 清理过期的客户端信息缓存
     */
    public void cleanupExpiredCache() {
        // 这里可以实现缓存清理策略
        log.debug("清理客户端信息缓存，当前缓存大小: {}", clientInfoCache.size());
    }
    
    /**
     * 获取客户端统计信息
     */
    public Map<String, Object> getClientStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedClients", clientInfoCache.size());
        stats.put("trustedIps", trustedIpRanges.size());
        
        // 统计设备类型
        long mobile = clientInfoCache.values().stream()
            .filter(ci -> ci.getDeviceType() == ClientInfo.DeviceType.MOBILE).count();
        long desktop = clientInfoCache.values().stream()
            .filter(ci -> ci.getDeviceType() == ClientInfo.DeviceType.DESKTOP).count();
        long tablet = clientInfoCache.values().stream()
            .filter(ci -> ci.getDeviceType() == ClientInfo.DeviceType.TABLET).count();
        long unknown = clientInfoCache.values().stream()
            .filter(ci -> ci.getDeviceType() == ClientInfo.DeviceType.UNKNOWN).count();
        
        stats.put("deviceStats", Map.of(
            "mobile", mobile,
            "desktop", desktop,
            "tablet", tablet,
            "unknown", unknown
        ));
        
        return stats;
    }
}