package com.group.gateway.auth.service.impl;

import com.group.gateway.auth.entity.User;
import com.group.gateway.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 * 实现Spring Security的UserDetailsService接口
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    
    @Autowired
    private UserService userService;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        return buildUserDetails(user);
    }
    
    /**
     * 根据用户ID加载用户详情
     */
    @Transactional
    public UserDetails loadUserById(String id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
        
        return buildUserDetails(user);
    }
    
    /**
     * 构建UserDetails对象
     */
    private UserDetails buildUserDetails(User user) {
        // 获取用户角色
        List<String> roleNames = userService.getUserRoleNames(user.getId());
        
        // 获取用户权限
        List<String> permissionNames = userService.getUserPermissionNames(user.getId());
        
        // 合并角色和权限
        List<SimpleGrantedAuthority> authorities = roleNames.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
        
        authorities.addAll(permissionNames.stream()
                .map(permission -> new SimpleGrantedAuthority(permission))
                .collect(Collectors.toList()));
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.LOCKED)
                .credentialsExpired(false)
                .disabled(user.getStatus() == User.UserStatus.DISABLED)
                .build();
    }
}