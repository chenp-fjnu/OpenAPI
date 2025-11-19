package com.group.gateway.auth.repository;

import com.group.gateway.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    /**
     * 根据角色代码查找角色
     * 
     * @param code 角色代码
     * @param tenantId 租户ID
     * @return 角色
     */
    Optional<Role> findByCodeAndTenantId(String code, String tenantId);
    
    /**
     * 根据租户ID查询所有角色
     * 
     * @param tenantId 租户ID
     * @return 角色列表
     */
    List<Role> findByTenantId(String tenantId);
    
    /**
     * 根据租户ID和状态查询角色
     * 
     * @param tenantId 租户ID
     * @param isActive 是否激活
     * @return 角色列表
     */
    List<Role> findByTenantIdAndIsActive(String tenantId, boolean isActive);
    
    /**
     * 根据租户ID和类型查询角色
     * 
     * @param tenantId 租户ID
     * @param type 角色类型
     * @return 角色列表
     */
    List<Role> findByTenantIdAndType(String tenantId, Role.RoleType type);
    
    /**
     * 检查角色代码是否存在
     * 
     * @param code 角色代码
     * @param tenantId 租户ID
     * @return 是否存在
     */
    boolean existsByCodeAndTenantId(String code, String tenantId);
    
    /**
     * 检查角色代码是否存在（排除指定ID）
     * 
     * @param code 角色代码
     * @param tenantId 租户ID
     * @param excludeId 排除的角色ID
     * @return 是否存在
     */
    boolean existsByCodeAndTenantIdAndIdNot(String code, String tenantId, String excludeId);
    
    /**
     * 根据关键字搜索角色
     * 
     * @param tenantId 租户ID
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Role> searchRoles(@Param("tenantId") String tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 获取默认角色列表
     * 
     * @param tenantId 租户ID
     * @return 默认角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.isDefault = true")
    List<Role> getDefaultRoles(@Param("tenantId") String tenantId);
    
    /**
     * 获取系统角色列表
     * 
     * @param tenantId 租户ID
     * @return 系统角色列表
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.isSystem = true")
    List<Role> getSystemRoles(@Param("tenantId") String tenantId);
    
    /**
     * 根据权限ID查询角色
     * 
     * @param permissionId 权限ID
     * @param tenantId 租户ID
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.permissions p WHERE p.id = :permissionId AND r.tenantId = :tenantId")
    List<Role> findByPermissionIdAndTenantId(@Param("permissionId") String permissionId, @Param("tenantId") String tenantId);
    
    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 角色列表
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.id = :userId AND r.tenantId = :tenantId")
    List<Role> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);
    
    /**
     * 根据租户ID统计角色数量
     * 
     * @param tenantId 租户ID
     * @return 角色数量
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * 根据租户ID和状态统计角色数量
     * 
     * @param tenantId 租户ID
     * @param isActive 是否激活
     * @return 角色数量
     */
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenantId = :tenantId AND r.isActive = :isActive")
    long countByTenantIdAndIsActive(@Param("tenantId") String tenantId, @Param("isActive") boolean isActive);
    
    /**
     * 批量设置角色状态
     * 
     * @param tenantId 租户ID
     * @param roleIds 角色ID列表
     * @param isActive 是否激活
     * @return 更新记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE Role r SET r.isActive = :isActive WHERE r.tenantId = :tenantId AND r.id IN :roleIds")
    int updateStatusByTenantIdAndIds(@Param("tenantId") String tenantId, @Param("roleIds") List<String> roleIds, @Param("isActive") boolean isActive);
    
    /**
     * 根据租户ID删除角色
     * 
     * @param tenantId 租户ID
     * @param roleIds 角色ID列表
     * @return 删除记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Role r WHERE r.tenantId = :tenantId AND r.id IN :roleIds")
    int deleteByTenantIdAndIds(@Param("tenantId") String tenantId, @Param("roleIds") List<String> roleIds);
    
    /**
     * 根据租户ID和排序规则获取所有角色
     * 
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 角色分页结果
     */
    Page<Role> findByTenantIdOrderBySortOrderAscNameAsc(String tenantId, Pageable pageable);
}