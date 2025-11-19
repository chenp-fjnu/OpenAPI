package com.group.gateway.auth.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * 用户实体类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户ID
     */
    private String id;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;
    
    /**
     * 手机号
     */
    @Size(min = 11, max = 11, message = "手机号格式不正确")
    private String phone;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    /**
     * 用户真实姓名
     */
    private String fullName;
    
    /**
     * 用户头像URL
     */
    private String avatar;
    
    /**
     * 用户状态：ACTIVE-活跃，INACTIVE-非活跃，BANNED-禁用，SUSPENDED-暂停
     */
    private UserStatus status;
    
    /**
     * 是否激活
     */
    private boolean enabled;
    
    /**
     * 是否账户未过期
     */
    private boolean accountNonExpired;
    
    /**
     * 是否账户未锁定
     */
    private boolean accountNonLocked;
    
    /**
     * 是否凭证未过期
     */
    private boolean credentialsNonExpired;
    
    /**
     * 用户角色集合
     */
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    
    /**
     * 用户权限集合
     */
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;
    
    /**
     * 登录失败次数
     */
    private int loginFailureCount;
    
    /**
     * 账户锁定时间
     */
    private LocalDateTime lockedTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 租户ID
     */
    private String tenantId;
    
    /**
     * 组织ID
     */
    private String organizationId;
    
    /**
     * 用户类型：INTERNAL-内部用户，EXTERNAL-外部用户，ADMIN-管理员
     */
    private UserType userType;
    
    /**
     * 多因子认证是否启用
     */
    private boolean mfaEnabled;
    
    /**
     * 多因子认证令牌
     */
    private String mfaSecret;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 刷新令牌过期时间
     */
    private LocalDateTime refreshTokenExpiryTime;
    
    /**
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE,    // 活跃
        INACTIVE,  // 非活跃
        BANNED,    // 禁用
        SUSPENDED  // 暂停
    }
    
    /**
     * 用户类型枚举
     */
    public enum UserType {
        INTERNAL,  // 内部用户
        EXTERNAL,  // 外部用户
        ADMIN      // 管理员
    }
    
    /**
     * 检查用户是否拥有指定角色
     */
    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * 检查用户是否拥有指定权限
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }
    
    /**
     * 检查用户是否拥有指定角色中的任意一个
     */
    public boolean hasAnyRole(String... roleNames) {
        for (String roleName : roleNames) {
            if (hasRole(roleName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查用户是否拥有指定权限中的任意一个
     */
    public boolean hasAnyPermission(String... permissionNames) {
        for (String permissionName : permissionNames) {
            if (hasPermission(permissionName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 添加角色
     */
    public void addRole(Role role) {
        this.roles.add(role);
    }
    
    /**
     * 移除角色
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
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