package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.User;
import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 更新用户请求DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class UpdateUserRequest {
    
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    private String avatar;
    
    private User.UserStatus status;
    
    private User.UserType userType;
    
    private String organizationId;
    
    private List<String> roleIds;
    
    private List<String> permissionIds;
    
    private Boolean mfaEnabled;
    
    private String preferredLanguage;
    
    private String timezone;
    
    private String department;
    
    private String position;
    
    private String managerId;
    
    private String emergencyContact;
    
    private String emergencyPhone;
}