package com.group.gateway.auth.service;

import com.group.gateway.auth.entity.User;
import io.jsonwebtoken.Claims;

import java.util.Map;

/**
 * 认证服务接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    AuthResult login(LoginRequest request);
    
    /**
     * 用户登出
     */
    void logout(LogoutRequest request);
    
    /**
     * 刷新令牌
     */
    AuthResult refreshToken(RefreshTokenRequest request);
    
    /**
     * 验证令牌
     */
    TokenValidationResult validateToken(String token);
    
    /**
     * 验证JWT令牌
     */
    TokenValidationResult validateJwtToken(String token);
    
    /**
     * 解析JWT令牌
     */
    Claims parseToken(String token);
    
    /**
     * 生成JWT令牌
     */
    String generateToken(User user);
    
    /**
     * 生成访问令牌
     */
    String generateAccessToken(User user);
    
    /**
     * 生成刷新令牌
     */
    String generateRefreshToken(User user);
    
    /**
     * 用户注册
     */
    UserResult register(RegisterRequest request);
    
    /**
     * 验证邮箱
     */
    boolean verifyEmail(String userId, String verificationCode);
    
    /**
     * 发送邮箱验证
     */
    void sendEmailVerification(String userId);
    
    /**
     * 重置密码请求
     */
    void requestPasswordReset(String email);
    
    /**
     * 重置密码
     */
    boolean resetPassword(PasswordResetRequest request);
    
    /**
     * 修改密码
     */
    boolean changePassword(ChangePasswordRequest request);
    
    /**
     * 启用多因子认证
     */
    MFAResult enableMFA(String userId);
    
    /**
     * 验证多因子认证
     */
    boolean verifyMFA(String userId, String mfaCode);
    
    /**
     * 禁用多因子认证
     */
    void disableMFA(String userId, String mfaCode);
    
    /**
     * 获取用户信息
     */
    UserInfo getUserInfo(String userId);
    
    /**
     * 更新用户信息
     */
    UserResult updateUserInfo(String userId, UpdateUserInfoRequest request);
    
    /**
     * 检查权限
     */
    boolean checkPermission(String userId, String permission);
    
    /**
     * 检查角色
     */
    boolean checkRole(String userId, String role);
    
    /**
     * 获取用户权限列表
     */
    Map<String, Boolean> getUserPermissions(String userId);
    
    /**
     * 获取用户角色列表
     */
    Map<String, Boolean> getUserRoles(String userId);
    
    /**
     * SSO登录
     */
    AuthResult ssoLogin(SSORequest request);
    
    /**
     * OAuth2登录
     */
    AuthResult oauth2Login(OAuth2LoginRequest request);
    
    /**
     * LDAP认证
     */
    AuthResult ldapAuthenticate(LDAPAuthRequest request);
    
    /**
     * 内部认证
     */
    AuthResult internalAuthenticate(InternalAuthRequest request);
    
    // === 请求/响应类 ===
    
    /**
     * 登录请求
     */
    class LoginRequest {
        private String username;
        private String password;
        private String tenantId;
        private String clientId;
        private String captcha;
        private String captchaKey;
        private boolean rememberMe;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getCaptcha() { return captcha; }
        public void setCaptcha(String captcha) { this.captcha = captcha; }
        public String getCaptchaKey() { return captchaKey; }
        public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }
        public boolean isRememberMe() { return rememberMe; }
        public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
    }
    
    /**
     * 登出请求
     */
    class LogoutRequest {
        private String userId;
        private String token;
        private String deviceId;
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }
    
    /**
     * 刷新令牌请求
     */
    class RefreshTokenRequest {
        private String refreshToken;
        private String userId;
        
        // Getters and Setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
    
    /**
     * 注册请求
     */
    class RegisterRequest {
        private String username;
        private String email;
        private String phone;
        private String password;
        private String confirmPassword;
        private String tenantId;
        private String verificationCode;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    }
    
    /**
     * 密码重置请求
     */
    class PasswordResetRequest {
        private String email;
        private String verificationCode;
        private String newPassword;
        private String confirmPassword;
        
        // Getters and Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getVerificationCode() { return verificationCode; }
        public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
    
    /**
     * 修改密码请求
     */
    class ChangePasswordRequest {
        private String userId;
        private String oldPassword;
        private String newPassword;
        private String confirmPassword;
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    }
    
    /**
     * 更新用户信息请求
     */
    class UpdateUserInfoRequest {
        private String fullName;
        private String phone;
        private String avatar;
        private User.UserType userType;
        
        // Getters and Setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public User.UserType getUserType() { return userType; }
        public void setUserType(User.UserType userType) { this.userType = userType; }
    }
    
    /**
     * SSO请求
     */
    class SSORequest {
        private String ssoToken;
        private String serviceUrl;
        private String tenantId;
        
        // Getters and Setters
        public String getSsoToken() { return ssoToken; }
        public void setSsoToken(String ssoToken) { this.ssoToken = ssoToken; }
        public String getServiceUrl() { return serviceUrl; }
        public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
    
    /**
     * OAuth2登录请求
     */
    class OAuth2LoginRequest {
        private String provider;
        private String authCode;
        private String state;
        private String redirectUri;
        private String tenantId;
        
        // Getters and Setters
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getAuthCode() { return authCode; }
        public void setAuthCode(String authCode) { this.authCode = authCode; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
    
    /**
     * LDAP认证请求
     */
    class LDAPAuthRequest {
        private String username;
        private String password;
        private String tenantId;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
    
    /**
     * 内部认证请求
     */
    class InternalAuthRequest {
        private String username;
        private String password;
        private String tenantId;
        private String clientType;
        
        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getClientType() { return clientType; }
        public void setClientType(String clientType) { this.clientType = clientType; }
    }
    
    /**
     * 认证结果
     */
    class AuthResult {
        private boolean success;
        private String message;
        private User user;
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
        private String tokenType;
        
        // Constructors
        public AuthResult() {}
        
        public AuthResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    }
    
    /**
     * 令牌验证结果
     */
    class TokenValidationResult {
        private boolean valid;
        private String userId;
        private String username;
        private String tenantId;
        private String errorMessage;
        
        // Constructors
        public TokenValidationResult() {}
        
        public TokenValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        // Getters and Setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 用户结果
     */
    class UserResult {
        private boolean success;
        private String message;
        private User user;
        
        // Constructors
        public UserResult() {}
        
        public UserResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public User getUser() { return user; }
        public void setUser(User user) { this.user = user; }
    }
    
    /**
     * MFA结果
     */
    class MFAResult {
        private boolean success;
        private String message;
        private String qrCode;
        private String secret;
        
        // Constructors
        public MFAResult() {}
        
        public MFAResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
    
    /**
     * 用户信息
     */
    class UserInfo {
        private String id;
        private String username;
        private String email;
        private String phone;
        private String fullName;
        private String avatar;
        private User.UserStatus status;
        private User.UserType userType;
        private String tenantId;
        private String organizationId;
        private Map<String, Boolean> roles;
        private Map<String, Boolean> permissions;
        private LocalDateTime lastLoginTime;
        private boolean mfaEnabled;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public User.UserStatus getStatus() { return status; }
        public void setStatus(User.UserStatus status) { this.status = status; }
        public User.UserType getUserType() { return userType; }
        public void setUserType(User.UserType userType) { this.userType = userType; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getOrganizationId() { return organizationId; }
        public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }
        public Map<String, Boolean> getRoles() { return roles; }
        public void setRoles(Map<String, Boolean> roles) { this.roles = roles; }
        public Map<String, Boolean> getPermissions() { return permissions; }
        public void setPermissions(Map<String, Boolean> permissions) { this.permissions = permissions; }
        public LocalDateTime getLastLoginTime() { return lastLoginTime; }
        public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
        public boolean isMfaEnabled() { return mfaEnabled; }
        public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    }
}