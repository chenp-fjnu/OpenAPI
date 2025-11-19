package com.group.gateway.auth.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    /**
     * 角色ID
     */
    private String id;
    
    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50个字符")
    private String name;
    
    /**
     * 角色描述
     */
    private String description;
    
    /**
     * 角色代码
     */
    @NotBlank(message = "角色代码不能为空")
    @Size(max = 20, message = "角色代码长度不能超过20个字符")
    private String code;
    
    /**
     * 角色类型：SYSTEM-系统角色，CUSTOM-自定义角色
     */
    private RoleType roleType;
    
    /**
     * 是否为默认角色
     */
    private boolean isDefault;
    
    /**
     * 是否启用
     */
    private boolean enabled;
    
    /**
     * 优先级（数值越小优先级越高）
     */
    private Integer priority;
    
    /**
     * 权限集合
     */
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
    
    /**
     * 租户ID（多租户场景）
     */
    private String tenantId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    private String createBy;
    
    /**
     * 更新人
     */
    private String updateBy;
    
    /**
     * 角色类型枚举
     */
    public enum RoleType {
        SYSTEM,   // 系统角色
        CUSTOM    // 自定义角色
    }
    
    /**
     * 检查角色是否拥有指定权限
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }
    
    /**
     * 添加权限
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }
    
    /**
     * 移除权限
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }
}