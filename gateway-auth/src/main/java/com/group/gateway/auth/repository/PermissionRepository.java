package com.group.gateway.auth.repository;

import com.group.gateway.auth.entity.Permission;
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
 * 权限数据访问层接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    
    /**
     * 根据权限代码查找权限
     * 
     * @param code 权限代码
     * @param tenantId 租户ID
     * @return 权限
     */
    Optional<Permission> findByCodeAndTenantId(String code, String tenantId);
    
    /**
     * 根据权限代码和父ID查找权限
     * 
     * @param code 权限代码
     * @param parentId 父权限ID
     * @param tenantId 租户ID
     * @return 权限
     */
    Optional<Permission> findByCodeAndParentIdAndTenantId(String code, String parentId, String tenantId);
    
    /**
     * 根据租户ID查询所有权限
     * 
     * @param tenantId 租户ID
     * @return 权限列表
     */
    List<Permission> findByTenantId(String tenantId);
    
    /**
     * 根据租户ID和父ID查询权限
     * 
     * @param tenantId 租户ID
     * @param parentId 父权限ID
     * @return 权限列表
     */
    List<Permission> findByTenantIdAndParentId(String tenantId, String parentId);
    
    /**
     * 根据租户ID和资源类型查询权限
     * 
     * @param tenantId 租户ID
     * @param resourceType 资源类型
     * @return 权限列表
     */
    List<Permission> findByTenantIdAndResourceType(String tenantId, Permission.ResourceType resourceType);
    
    /**
     * 根据租户ID和权限类型查询权限
     * 
     * @param tenantId 租户ID
     * @param permissionType 权限类型
     * @return 权限列表
     */
    List<Permission> findByTenantIdAndPermissionType(String tenantId, Permission.PermissionType permissionType);
    
    /**
     * 根据租户ID和资源路径查询权限
     * 
     * @param tenantId 租户ID
     * @param resourcePath 资源路径
     * @return 权限列表
     */
    List<Permission> findByTenantIdAndResourcePath(String tenantId, String resourcePath);
    
    /**
     * 根据租户ID和HTTP方法查询权限
     * 
     * @param tenantId 租户ID
     * @param httpMethod HTTP方法
     * @return 权限列表
     */
    List<Permission> findByTenantIdAndHttpMethod(String tenantId, String httpMethod);
    
    /**
     * 检查权限代码是否存在
     * 
     * @param code 权限代码
     * @param tenantId 租户ID
     * @return 是否存在
     */
    boolean existsByCodeAndTenantId(String code, String tenantId);
    
    /**
     * 检查权限代码是否存在（排除指定ID）
     * 
     * @param code 权限代码
     * @param tenantId 租户ID
     * @param excludeId 排除的权限ID
     * @return 是否存在
     */
    boolean existsByCodeAndTenantIdAndIdNot(String code, String tenantId, String excludeId);
    
    /**
     * 根据关键字搜索权限
     * 
     * @param tenantId 租户ID
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(p.resourcePath) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Permission> searchPermissions(@Param("tenantId") String tenantId, @Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 获取根权限列表
     * 
     * @param tenantId 租户ID
     * @return 根权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId AND p.parentId IS NULL ORDER BY p.sortOrder ASC, p.name ASC")
    List<Permission> getRootPermissions(@Param("tenantId") String tenantId);
    
    /**
     * 获取叶子权限列表
     * 
     * @param tenantId 租户ID
     * @return 叶子权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId AND NOT EXISTS (SELECT 1 FROM Permission child WHERE child.parentId = p.id)")
    List<Permission> getLeafPermissions(@Param("tenantId") String tenantId);
    
    /**
     * 获取菜单权限列表
     * 
     * @param tenantId 租户ID
     * @return 菜单权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId AND p.resourceType = 'MENU' ORDER BY p.sortOrder ASC, p.name ASC")
    List<Permission> getMenuPermissions(@Param("tenantId") String tenantId);
    
    /**
     * 获取API权限列表
     * 
     * @param tenantId 租户ID
     * @return API权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId AND p.resourceType = 'API' ORDER BY p.sortOrder ASC, p.name ASC")
    List<Permission> getApiPermissions(@Param("tenantId") String tenantId);
    
    /**
     * 根据角色ID查询权限
     * 
     * @param roleId 角色ID
     * @param tenantId 租户ID
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.id = :roleId AND p.tenantId = :tenantId ORDER BY p.sortOrder ASC, p.name ASC")
    List<Permission> findByRoleIdAndTenantId(@Param("roleId") String roleId, @Param("tenantId") String tenantId);
    
    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @param tenantId 租户ID
     * @return 权限列表
     */
    @Query("SELECT DISTINCT p FROM Permission p JOIN p.roles r JOIN r.users u WHERE u.id = :userId AND p.tenantId = :tenantId ORDER BY p.sortOrder ASC, p.name ASC")
    List<Permission> findByUserIdAndTenantId(@Param("userId") String userId, @Param("tenantId") String tenantId);
    
    /**
     * 根据资源路径和HTTP方法查询权限
     * 
     * @param resourcePath 资源路径
     * @param httpMethod HTTP方法
     * @param tenantId 租户ID
     * @return 权限列表
     */
    @Query("SELECT p FROM Permission p WHERE p.resourcePath = :resourcePath AND p.httpMethod = :httpMethod AND p.tenantId = :tenantId")
    List<Permission> findByResourcePathAndHttpMethodAndTenantId(@Param("resourcePath") String resourcePath, @Param("httpMethod") String httpMethod, @Param("tenantId") String tenantId);
    
    /**
     * 根据租户ID统计权限数量
     * 
     * @param tenantId 租户ID
     * @return 权限数量
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);
    
    /**
     * 根据租户ID和资源类型统计权限数量
     * 
     * @param tenantId 租户ID
     * @param resourceType 资源类型
     * @return 权限数量
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.tenantId = :tenantId AND p.resourceType = :resourceType")
    long countByTenantIdAndResourceType(@Param("tenantId") String tenantId, @Param("resourceType") Permission.ResourceType resourceType);
    
    /**
     * 根据租户ID和父ID统计权限数量
     * 
     * @param tenantId 租户ID
     * @param parentId 父权限ID
     * @return 权限数量
     */
    @Query("SELECT COUNT(p) FROM Permission p WHERE p.tenantId = :tenantId AND p.parentId = :parentId")
    long countByTenantIdAndParentId(@Param("tenantId") String tenantId, @Param("parentId") String parentId);
    
    /**
     * 批量设置权限状态
     * 
     * @param tenantId 租户ID
     * @param permissionIds 权限ID列表
     * @param isActive 是否激活
     * @return 更新记录数
     */
    @Modifying
    @Transactional
    @Query("UPDATE Permission p SET p.isActive = :isActive WHERE p.tenantId = :tenantId AND p.id IN :permissionIds")
    int updateStatusByTenantIdAndIds(@Param("tenantId") String tenantId, @Param("permissionIds") List<String> permissionIds, @Param("isActive") boolean isActive);
    
    /**
     * 根据租户ID删除权限
     * 
     * @param tenantId 租户ID
     * @param permissionIds 权限ID列表
     * @return 删除记录数
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Permission p WHERE p.tenantId = :tenantId AND p.id IN :permissionIds")
    int deleteByTenantIdAndIds(@Param("tenantId") String tenantId, @Param("permissionIds") List<String> permissionIds);
    
    /**
     * 根据租户ID和排序规则获取所有权限
     * 
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 权限分页结果
     */
    Page<Permission> findByTenantIdOrderBySortOrderAscNameAsc(String tenantId, Pageable pageable);
}