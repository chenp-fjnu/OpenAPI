package com.group.gateway.auth.service.impl;

import com.group.gateway.auth.entity.User;
import com.group.gateway.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * UserDetailsService实现类
 * 为Spring Security提供用户详情加载服务
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserService userService;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        try {
            User user = userService.findByUsername(usernameOrEmail);
            if (user == null) {
                // 尝试通过邮箱查找
                user = userService.findByEmail(usernameOrEmail);
            }
            
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: " + usernameOrEmail);
            }
            
            if (!user.getEnabled()) {
                throw new UsernameNotFoundException("用户已被禁用: " + usernameOrEmail);
            }
            
            // 获取用户角色和权限
            List<String> roles = userService.getUserRoles(user.getId());
            List<String> permissions = userService.getUserPermissions(user.getId());
            
            // 构建权限列表
            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
            
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.getAccountNonExpired())
                .accountLocked(!user.getAccountNonLocked())
                .credentialsExpired(!user.getCredentialsNonExpired())
                .disabled(!user.getEnabled())
                .build();
                
        } catch (Exception e) {
            throw new UsernameNotFoundException("用户加载失败: " + usernameOrEmail, e);
        }
    }
    
    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        try {
            User user = userService.findById(userId);
            if (user == null) {
                throw new UsernameNotFoundException("用户不存在: ID " + userId);
            }
            
            if (!user.getEnabled()) {
                throw new UsernameNotFoundException("用户已被禁用: ID " + userId);
            }
            
            // 获取用户角色和权限
            List<String> roles = userService.getUserRoles(userId);
            List<String> permissions = userService.getUserPermissions(userId);
            
            // 构建权限列表
            List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            authorities.addAll(permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()));
            
            return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(!user.getAccountNonExpired())
                .accountLocked(!user.getAccountNonLocked())
                .credentialsExpired(!user.getCredentialsNonExpired())
                .disabled(!user.getEnabled())
                .build();
                
        } catch (Exception e) {
            throw new UsernameNotFoundException("用户加载失败: ID " + userId, e);
        }
    }
}