package com.group.gateway.auth.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 令牌响应DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class TokenResponse {
    
    private String accessToken;
    
    private String refreshToken;
    
    private String tokenType = "Bearer";
    
    private Long expiresIn;
    
    private Long refreshExpiresIn;
    
    private String scope;
    
    private String tenantId;
    
    private String userId;
    
    private String username;
    
    private List<String> roles;
    
    private List<String> permissions;
    
    private Map<String, Object> customClaims;
    
    private String mfaRequired;
    
    private String sessionId;
    
    private Boolean needsPasswordChange;
    
    private String passwordExpiryDate;
}