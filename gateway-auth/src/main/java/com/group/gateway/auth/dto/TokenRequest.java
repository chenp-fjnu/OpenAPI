package com.group.gateway.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 令牌请求DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class TokenRequest {
    
    @NotBlank(message = "令牌不能为空")
    private String token;
    
    private String refreshToken;
    
    private String clientId;
    
    private String clientSecret;
    
    private String grantType; // refresh_token, revoke_token
    
    private String tenantId;
    
    private String ipAddress;
    
    private String userAgent;
}