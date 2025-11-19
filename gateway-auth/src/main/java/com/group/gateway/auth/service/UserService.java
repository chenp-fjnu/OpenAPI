package com.group.gateway.auth.service;

import com.group.gateway.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 用户服务接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
public interface UserService {
    
    /**
     * 创建用户
     */
    User createUser(User user);
    
    /**
     * 根据ID获取用户
     */
    Optional<User> getUserById(String id);
    
    /**
     * 根据用户名获取用户
     */
    Optional<User> getUserByUsername(String username);
    
    /**
     * 根据邮箱获取用户
     */
    Optional<User> getUserByEmail(String email);
    
    /**
     * 根据手机号获取用户
     */
    Optional<User> getUserByPhone(String phone);
    
    /**
     * 更新用户
     */
    User updateUser(String id, User user);
    
    /**
     * 删除用户
     */
    void deleteUser(String id);
    
    /**
     * 分页查询用户
     */
    Page<User> getUsers(Pageable pageable);
    
    /**
     * 根据状态查询用户
     */
    List<User> getUsersByStatus(User.UserStatus status);
    
    /**
     * 根据租户ID查询用户
     */
    List<User> getUsersByTenantId(String tenantId);
    
    /**
     * 根据组织ID查询用户
     */
    List<User> getUsersByOrganizationId(String organizationId);
    
    /**
     * 验证用户密码
     */
    boolean validatePassword(String rawPassword, String encodedPassword);
    
    /**
     * 加密密码
     */
    String encodePassword(String rawPassword);
    
    /**
     * 用户登录
     */
    Optional<User> login(String username, String password);
    
    /**
     * 用户登出
     */
    void logout(String userId);
    
    /**
     * 修改密码
     */
    boolean changePassword(String userId, String oldPassword, String newPassword);
    
    /**
     * 重置密码
     */
    void resetPassword(String userId, String newPassword);
    
    /**
     * 启用用户
     */
    void enableUser(String id);
    
    /**
     * 禁用用户
     */
    void disableUser(String id);
    
    /**
     * 锁定用户
     */
    void lockUser(String id);
    
    /**
     * 解锁用户
     */
    void unlockUser(String id);
    
    /**
     * 增加登录失败次数
     */
    void incrementLoginFailureCount(String username);
    
    /**
     * 重置登录失败次数
     */
    void resetLoginFailureCount(String username);
    
    /**
     * 检查用户是否被锁定
     */
    boolean isUserLocked(String username);
    
    /**
     * 生成刷新令牌
     */
    String generateRefreshToken(User user);
    
    /**
     * 验证刷新令牌
     */
    boolean validateRefreshToken(String token, String userId);
    
    /**
     * 撤销刷新令牌
     */
    void revokeRefreshToken(String userId);
    
    /**
     * 获取用户角色
     */
    List<String> getUserRoleNames(String userId);
    
    /**
     * 获取用户权限
     */
    List<String> getUserPermissionNames(String userId);
    
    /**
     * 检查用户是否有指定权限
     */
    boolean hasPermission(String userId, String permissionName);
    
    /**
     * 检查用户是否有指定角色
     */
    boolean hasRole(String userId, String roleName);
    
    /**
     * 检查用户是否激活
     */
    boolean isEnabled(String userId);
    
    /**
     * 更新最后登录时间
     */
    void updateLastLoginTime(String userId);
    
    /**
     * 批量导入用户
     */
    List<User> batchCreateUsers(List<User> users);
    
    /**
     * 搜索用户
     */
    Page<User> searchUsers(String keyword, Pageable pageable);
}