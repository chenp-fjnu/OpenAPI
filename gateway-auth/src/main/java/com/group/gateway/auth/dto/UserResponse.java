package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.User;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户响应DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class UserResponse {
    
    private String id;
    
    private String username;
    
    private String email;
    
    private String phone;
    
    private String fullName;
    
    private String avatar;
    
    private String tenantId;
    
    private String organizationId;
    
    private User.UserStatus status;
    
    private User.UserType userType;
    
    private String department;
    
    private String position;
    
    private String managerId;
    
    private String preferredLanguage;
    
    private String timezone;
    
    private LocalDateTime lastLoginAt;
    
    private LocalDateTime passwordLastChanged;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private Boolean mfaEnabled;
    
    private Integer loginFailureCount;
    
    private List<RoleResponse> roles;
    
    private List<PermissionResponse> permissions;
    
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setFullName(user.getFullName());
        response.setAvatar(user.getAvatar());
        response.setTenantId(user.getTenantId());
        response.setOrganizationId(user.getOrganizationId());
        response.setStatus(user.getStatus());
        response.setUserType(user.getUserType());
        response.setDepartment(user.getDepartment());
        response.setPosition(user.getPosition());
        response.setManagerId(user.getManagerId());
        response.setPreferredLanguage(user.getPreferredLanguage());
        response.setTimezone(user.getTimezone());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setPasswordLastChanged(user.getPasswordLastChanged());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setMfaEnabled(user.getMfaEnabled());
        response.setLoginFailureCount(user.getLoginFailureCount());
        
        if (user.getRoles() != null) {
            response.setRoles(user.getRoles().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList()));
        }
        
        if (user.getPermissions() != null) {
            response.setPermissions(user.getPermissions().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList()));
        }
        
        return response;
    }
}