package com.yy.homi.gateway.config;

import com.yy.homi.gateway.filters.AuthGlobalFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity //必须使用这个注解，而不是 EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthGlobalFilter authGlobalFilter;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // 2. 在 WebFlux 中，配置方式从 http.xxx() 变成了 ServerHttpSecurity 的链式调用
        return http
                // 禁用 CSRF,和允许跨域
                .csrf().disable()
                .cors()
                // 无状态配置：WebFlux 默认就是无状态的，不需要显式配置 SessionCreationPolicy.STATELESS
                .and()
                .addFilterBefore(authGlobalFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange()
                // 放行 OPTIONS
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 放行登录相关路径 (注意：WebFlux 下使用 pathMatchers 而非 antMatchers)
                .pathMatchers("/oauth/login", "/oauth/token").permitAll()
                // 网关的职责通常是：放行所有，由 AuthGlobalFilter 统一拦截
                // 如果你想在网关层做更细的权限限制，才在这里配置 authenticated()
                .anyExchange().permitAll()

                .and()
                .build();
    }

    //空的UserDetailService，防止生成security password
//    @Bean
//    public MapReactiveUserDetailsService userDetailsService() {
//        // 创建一个永远不会被用到的虚拟用户
//        UserDetails user = User.withDefaultPasswordEncoder()
//                .username("prevent-password-generation")
//                .password("prevent-password-generation")
//                .roles("NONE")
//                .build();
//
//        // 只要 List 不为空，MapReactiveUserDetailsService 就会正常初始化
//        // 同时也因为有了这个 Bean，Spring 不再生成随机密码
//        return new MapReactiveUserDetailsService(user);
//    }
}