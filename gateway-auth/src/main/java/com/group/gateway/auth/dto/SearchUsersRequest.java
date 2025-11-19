package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.User;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 搜索用户请求DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class SearchUsersRequest {
    
    @Size(max = 100, message = "搜索关键词长度不能超过100个字符")
    private String keyword;
    
    private String tenantId;
    
    private String organizationId;
    
    private User.UserStatus status;
    
    private User.UserType userType;
    
    private List<String> roleIds;
    
    private List<String> permissionIds;
    
    private String department;
    
    private String position;
    
    private String managerId;
    
    private Boolean mfaEnabled;
    
    private Boolean hasRecentLogin;
    
    private Integer minLoginFailureCount;
    
    private Integer maxLoginFailureCount;
}