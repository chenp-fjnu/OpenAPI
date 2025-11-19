package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.Permission;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限响应DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class PermissionResponse {
    
    private String id;
    
    private String name;
    
    private String code;
    
    private String description;
    
    private Permission.ResourceType resourceType;
    
    private Permission.PermissionType permissionType;
    
    private String resourcePath;
    
    private String httpMethod;
    
    private String apiVersion;
    
    private Permission.ResourceScope scope;
    
    private Boolean isDefault;
    
    private Boolean isSystem;
    
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private List<PermissionResponse> children;
    
    private Integer roleCount;
    
    public static PermissionResponse from(Permission permission) {
        if (permission == null) {
            return null;
        }
        
        PermissionResponse response = new PermissionResponse();
        response.setId(permission.getId());
        response.setName(permission.getName());
        response.setCode(permission.getCode());
        response.setDescription(permission.getDescription());
        response.setResourceType(permission.getResourceType());
        response.setPermissionType(permission.getPermissionType());
        response.setResourcePath(permission.getResourcePath());
        response.setHttpMethod(permission.getHttpMethod());
        response.setApiVersion(permission.getApiVersion());
        response.setScope(permission.getScope());
        response.setIsDefault(permission.getIsDefault());
        response.setIsSystem(permission.getIsSystem());
        response.setSortOrder(permission.getSortOrder());
        response.setCreatedAt(permission.getCreatedAt());
        response.setUpdatedAt(permission.getUpdatedAt());
        
        if (permission.getChildren() != null) {
            response.setChildren(permission.getChildren().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList()));
        }
        
        return response;
    }
}