package com.group.gateway.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.group.gateway.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户认证相关API")
@Validated
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录获取访问令牌")
    public ResponseEntity<?> login(@Valid @RequestBody AuthService.LoginRequest request) {
        log.info("用户登录请求: {}", request.getUsername());
        
        AuthService.AuthResult result = authService.login(request);
        
        if (result.isSuccess()) {
            log.info("用户登录成功: {}", request.getUsername());
            return ResponseEntity.ok(result);
        } else {
            log.warn("用户登录失败: {}", result.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出并失效令牌")
    public ResponseEntity<?> logout(@Valid @RequestBody AuthService.LogoutRequest request) {
        log.info("用户登出请求: {}", request.getUserId());
        
        authService.logout(request);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "登出成功"
        ));
    }
    
    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody AuthService.RefreshTokenRequest request) {
        log.info("刷新令牌请求");
        
        AuthService.AuthResult result = authService.refreshToken(request);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证访问令牌的有效性")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        AuthService.TokenValidationResult result = authService.validateToken(token);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取用户信息
     */
    @GetMapping("/userinfo")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public ResponseEntity<?> getUserInfo(@RequestParam String userId) {
        AuthService.UserInfo userInfo = authService.getUserInfo(userId);
        
        return ResponseEntity.ok(userInfo);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册")
    public ResponseEntity<?> register(@Valid @RequestBody AuthService.RegisterRequest request) {
        log.info("用户注册请求: {}", request.getUsername());
        
        AuthService.UserResult result = authService.register(request);
        
        if (result.isSuccess()) {
            log.info("用户注册成功: {}", request.getUsername());
            return ResponseEntity.ok(result);
        } else {
            log.warn("用户注册失败: {}", result.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "用户修改登录密码")
    public ResponseEntity<?> changePassword(@Valid @RequestBody AuthService.ChangePasswordRequest request) {
        log.info("用户修改密码请求: {}", request.getUserId());
        
        boolean success = authService.changePassword(request);
        
        if (success) {
            log.info("用户修改密码成功: {}", request.getUserId());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码修改成功"
            ));
        } else {
            log.warn("用户修改密码失败");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "密码修改失败"
            ));
        }
    }
    
    /**
     * 忘记密码
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "请求重置密码")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        log.info("忘记密码请求: {}", email);
        
        authService.requestPasswordReset(email);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "密码重置邮件已发送"
        ));
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "使用验证码重置密码")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody AuthService.PasswordResetRequest request) {
        log.info("重置密码请求: {}", request.getEmail());
        
        boolean success = authService.resetPassword(request);
        
        if (success) {
            log.info("重置密码成功: {}", request.getEmail());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码重置成功"
            ));
        } else {
            log.warn("重置密码失败");
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "密码重置失败"
            ));
        }
    }
    
    /**
     * 启用MFA
     */
    @PostMapping("/enable-mfa")
    @Operation(summary = "启用多因子认证", description = "为用户账户启用多因子认证")
    public ResponseEntity<?> enableMFA(@RequestParam String userId) {
        log.info("启用MFA请求: {}", userId);
        
        AuthService.MFAResult result = authService.enableMFA(userId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * 验证MFA
     */
    @PostMapping("/verify-mfa")
    @Operation(summary = "验证多因子认证", description = "验证MFA验证码")
    public ResponseEntity<?> verifyMFA(@RequestParam String userId, 
                                      @RequestParam String mfaCode) {
        log.info("验证MFA请求: {}", userId);
        
        boolean success = authService.verifyMFA(userId, mfaCode);
        
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "MFA验证成功" : "MFA验证失败"
        ));
    }
    
    /**
     * 禁用MFA
     */
    @PostMapping("/disable-mfa")
    @Operation(summary = "禁用多因子认证", description = "禁用用户账户的多因子认证")
    public ResponseEntity<?> disableMFA(@RequestParam String userId, 
                                       @RequestParam String mfaCode) {
        log.info("禁用MFA请求: {}", userId);
        
        authService.disableMFA(userId, mfaCode);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "MFA已禁用"
        ));
    }
    
    /**
     * 检查权限
     */
    @GetMapping("/check-permission")
    @Operation(summary = "检查权限", description = "检查用户是否有指定权限")
    public ResponseEntity<?> checkPermission(@RequestParam String userId, 
                                           @RequestParam String permission) {
        boolean hasPermission = authService.checkPermission(userId, permission);
        
        return ResponseEntity.ok(Map.of(
            "hasPermission", hasPermission
        ));
    }
    
    /**
     * 检查角色
     */
    @GetMapping("/check-role")
    @Operation(summary = "检查角色", description = "检查用户是否有指定角色")
    public ResponseEntity<?> checkRole(@RequestParam String userId, 
                                     @RequestParam String role) {
        boolean hasRole = authService.checkRole(userId, role);
        
        return ResponseEntity.ok(Map.of(
            "hasRole", hasRole
        ));
    }
    
    /**
     * 获取用户权限
     */
    @GetMapping("/permissions")
    @Operation(summary = "获取用户权限", description = "获取用户的所有权限")
    public ResponseEntity<?> getUserPermissions(@RequestParam String userId) {
        Map<String, Boolean> permissions = authService.getUserPermissions(userId);
        
        return ResponseEntity.ok(permissions);
    }
    
    /**
     * 获取用户角色
     */
    @GetMapping("/roles")
    @Operation(summary = "获取用户角色", description = "获取用户的所有角色")
    public ResponseEntity<?> getUserRoles(@RequestParam String userId) {
        Map<String, Boolean> roles = authService.getUserRoles(userId);
        
        return ResponseEntity.ok(roles);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/userinfo")
    @Operation(summary = "更新用户信息", description = "更新当前用户的个人信息")
    public ResponseEntity<?> updateUserInfo(@RequestParam String userId,
                                           @Valid @RequestBody AuthService.UpdateUserInfoRequest request) {
        log.info("更新用户信息请求: {}", userId);
        
        AuthService.UserResult result = authService.updateUserInfo(userId, request);
        
        if (result.isSuccess()) {
            log.info("用户信息更新成功: {}", userId);
            return ResponseEntity.ok(result);
        } else {
            log.warn("用户信息更新失败: {}", result.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }
}