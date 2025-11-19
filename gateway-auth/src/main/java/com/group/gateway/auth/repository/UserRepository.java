package com.group.gateway.auth.repository;

import com.group.gateway.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);
    
    /**
     * 根据租户ID查找用户
     */
    List<User> findByTenantId(String tenantId);
    
    /**
     * 根据组织ID查找用户
     */
    List<User> findByOrganizationId(String organizationId);
    
    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(User.UserStatus status);
    
    /**
     * 根据角色ID查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.id = :roleId")
    List<User> findByRoleId(@Param("roleId") String roleId);
    
    /**
     * 根据权限代码查找用户
     */
    @Query("SELECT u FROM User u JOIN u.roles r JOIN r.permissions p WHERE p.code = :permissionCode")
    List<User> findByPermissionCode(@Param("permissionCode") String permissionCode);
    
    /**
     * 搜索用户
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);
    
    /**
     * 获取总用户数
     */
    @Query("SELECT COUNT(u) FROM User u")
    long getTotalUsers();
    
    /**
     * 获取活跃用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long getUsersByStatusCount(@Param("status") User.UserStatus status);
    
    /**
     * 根据租户获取用户数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId")
    long getUsersByTenantCount(@Param("tenantId") String tenantId);
    
    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginTime IS NOT NULL ORDER BY u.lastLoginTime DESC")
    List<User> findRecentlyLoggedInUsers(Pageable pageable);
}