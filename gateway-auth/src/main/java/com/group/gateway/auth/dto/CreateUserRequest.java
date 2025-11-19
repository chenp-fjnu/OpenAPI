package com.group.gateway.auth.dto;

import com.group.gateway.auth.entity.User;
import lombok.Data;

import javax.validation.constraints.*;
import java.util.List;

/**
 * 创建用户请求DTO
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Data
public class CreateUserRequest {
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间")
    private String password;
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    @Size(max = 100, message = "姓名长度不能超过100个字符")
    private String fullName;
    
    private String avatar;
    
    private String tenantId;
    
    private String organizationId;
    
    private User.UserType userType = User.UserType.INTERNAL;
    
    private User.UserStatus status = User.UserStatus.ACTIVE;
    
    private List<String> roleIds;
    
    private List<String> permissionIds;
    
    private boolean sendWelcomeEmail = false;
    
    private boolean requirePasswordChange = true;
}