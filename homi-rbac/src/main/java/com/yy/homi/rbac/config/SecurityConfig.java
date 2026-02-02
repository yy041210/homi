package com.yy.homi.rbac.config;

import com.yy.homi.rbac.filters.HeaderAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private HeaderAuthenticationFilter headerAuthenticationFilter;

    //1.配置密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http     // 1. 关键：必须禁用 CSRF 才能接收 Vue 发来的 POST 请求
                .csrf().disable()
                // 2. 开启跨域支持
                .cors()
                .and()
                .authorizeRequests()
                // 显式允许 OAuth2 的授权端点
//                .antMatchers("/oauth/**", "/login/**", "/logout/**").permitAll()
//                .antMatchers("/r/p1/**").hasAnyAuthority("p1")  //有p1权限才能访问
//                .antMatchers("/r/p2/**").hasAnyAuthority("p2")  //有p2权限才能访问
//                .antMatchers("/r/**").authenticated()  //匹配/r/**的请求必须认证通过
//                .anyRequest().authenticated()   //除了上述匹配到的请求，其他所有请求必须认证通过
                .anyRequest().permitAll()  //全部放行，有网关阻拦
                .and()
                .formLogin() //允许表单登录
                //.successForwardUrl("/loginsuccess")//自定义登录成功的页面地址（转发）
//                .defaultSuccessUrl("/loginsuccess") ;//重定向 成功url;
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS); //使用jwt方式不许Session了
//                .logout()
//                .logoutUrl("/logout") //自定义登出请求地址
//                .logoutSuccessUrl("/logoutsuccess");  //重定向的url

        http.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }

    //3.注册认证管理器(OAuth2.0密码模式必须用到)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 配置认证构造器
     * 告诉 Spring 使用自定义的 userDetailsService 和加密器
     */
//    @Bean
//    public DaoAuthenticationProvider authenticationProvider() {
//        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
//        authProvider.setUserDetailsService(userDetailsService); // 接入你的 SysUserDetailsServiceImpl
//        authProvider.setPasswordEncoder(passwordEncoder());     // 使用 BCrypt
//        return authProvider;
//    }
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder());
    }

}
