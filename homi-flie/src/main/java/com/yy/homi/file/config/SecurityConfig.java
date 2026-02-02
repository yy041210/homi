package com.yy.homi.file.config;

import com.yy.homi.file.filters.HeaderAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true) // 开启后，你依然可以用 @PreAuthorize
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private HeaderAuthenticationFilter headerAuthenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 1. 必须禁用 CSRF，否则文件上传接口（POST）会报 403
                .csrf().disable()
                .cors()  //允许跨域
                .and()
                // 2. 既然是微服务，关闭 Session，改为无状态
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 3. 放行所有 URL
                .authorizeRequests()
                .anyRequest().permitAll();

        // 4. 在过滤器链中加入你的“身份解析器”
        // 这样即使是 permitAll，SecurityContext 也会被你的 Filter 填充
        http.addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        /**
         * 1.你的 Filter：抢先解析网关 Header，把 userId: 101 塞进上下文。
         *
         * 2.UsernamePassword... Filter：看到已经有 101 的身份了，直接不管，放行。
         *
         * 3.Anonymous Filter：看到已经有身份了，不再分配“匿名用户”标签，放行。
         *
         * 4.FilterSecurityInterceptor：检查配置发现是 permitAll，挥挥手放行。
         *
         * 5.Controller：执行 UserContext.get()，成功拿到 101。
         */
    }

    //UserDetailService的bean，有了该bean,非响应式security就知道在哪查用户了，他就不会再给你生成security password了
    @Bean
    public UserDetailsService userDetailsService(){
        return new InMemoryUserDetailsManager(Collections.emptyList());
    }
}