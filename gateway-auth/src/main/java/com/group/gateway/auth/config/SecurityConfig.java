package com.group.gateway.auth.config;

import com.group.gateway.auth.security.JwtAuthenticationEntryPoint;
import com.group.gateway.auth.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置类
 * 
 * @author Group Gateway Team
 * @version 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * 认证管理器
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    /**
     * CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * 配置HTTP安全
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and.csrf().disable()
            // 异常处理
            .exceptionHandling()
            .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            // 会话管理
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 授权配置
            .authorizeRequests()
            // 公开接口
            .antMatchers("/auth/public/**").permitAll()
            .antMatchers("/auth/health").permitAll()
            .antMatchers("/auth/actuator/**").permitAll()
            .antMatchers(HttpMethod.GET, "/auth/api-docs/**").permitAll()
            .antMatchers("/auth/swagger-ui/**").permitAll()
            .antMatchers("/auth/v3/api-docs/**").permitAll()
            .antMatchers("/auth/swagger-ui.html").permitAll()
            // 认证接口
            .antMatchers("/auth/login").permitAll()
            .antMatchers("/auth/register").permitAll()
            .antMatchers("/auth/refresh").permitAll()
            .antMatchers("/auth/forgot-password").permitAll()
            .antMatchers("/auth/reset-password").permitAll()
            // 管理接口需要管理员权限
            .antMatchers("/auth/admin/**").hasRole("ADMIN")
            // API接口需要认证
            .anyRequest().authenticated();
        
        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        // 禁用缓存
        http.headers().cacheControl();
    }
}