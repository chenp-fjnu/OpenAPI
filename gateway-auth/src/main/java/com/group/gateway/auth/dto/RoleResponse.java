package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.Role;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色响应DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class RoleResponse {
    
    private String id;
    
    private String name;
    
    private String code;
    
    private String description;
    
    private Role.RoleType type;
    
    private Boolean isDefault;
    
    private Boolean isSystem;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<PermissionResponse> permissions;
    
    private Integer userCount;
    
    public static RoleResponse from(Role role) {
        if (role == null) {
            return null;
        }
        
        RoleResponse response = new RoleResponse();
        response.setId(role.getId());
        response.setName(role.getName());
        response.setCode(role.getCode());
        response.setDescription(role.getDescription());
        response.setType(role.getType());
        response.setIsDefault(role.getIsDefault());
        response.setIsSystem(role.getIsSystem());
        response.setSortOrder(role.getSortOrder());
        response.setCreatedAt(role.getCreatedAt());
        response.setUpdatedAt(role.getUpdatedAt());
        
        if (role.getPermissions() != null) {
            response.setPermissions(role.getPermissions().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList()));
        }
        
        return response;
    }
}