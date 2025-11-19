package com.group.gateway.auth.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 权限实体类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    
    /**
     * 权限ID
     */
    private String id;
    
    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String name;
    
    /**
     * 权限代码
     */
    @NotBlank(message = "权限代码不能为空")
    @Size(max = 50, message = "权限代码长度不能超过50个字符")
    private String code;
    
    /**
     * 权限描述
     */
    private String description;
    
    /**
     * 资源类型：API-接口资源，PAGE-页面资源，ELEMENT-页面元素资源
     */
    private ResourceType resourceType;
    
    /**
     * 权限类型：READ-读权限，WRITE-写权限，DELETE-删除权限，EXECUTE-执行权限
     */
    private PermissionType permissionType;
    
    /**
     * 资源路径（对于API资源）
     */
    private String resourcePath;
    
    /**
     * HTTP方法（对于API资源）
     */
    private String httpMethod;
    
    /**
     * 父权限ID（用于权限树结构）
     */
    private String parentId;
    
    /**
     * 权限层级
     */
    private Integer level;
    
    /**
     * 排序号
     */
    private Integer sortOrder;
    
    /**
     * 是否为叶子节点
     */
    private boolean leaf;
    
    /**
     * 权限状态：ENABLED-启用，DISABLED-禁用
     */
    private PermissionStatus status;
    
    /**
     * 是否为系统权限（不可删除）
     */
    private boolean systemPermission;
    
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
     * 租户ID（多租户场景）
     */
    private String tenantId;
    
    /**
     * 资源类型枚举
     */
    public enum ResourceType {
        API,      // 接口资源
        PAGE,     // 页面资源
        ELEMENT   // 页面元素资源
    }
    
    /**
     * 权限类型枚举
     */
    public enum PermissionType {
        READ,     // 读权限
        WRITE,    // 写权限
        DELETE,   // 删除权限
        EXECUTE   // 执行权限
    }
    
    /**
     * 权限状态枚举
     */
    public enum PermissionStatus {
        ENABLED,   // 启用
        DISABLED   // 禁用
    }
}