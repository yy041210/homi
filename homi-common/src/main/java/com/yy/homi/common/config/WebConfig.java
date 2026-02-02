package com.yy.homi.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 允许跨域访问的路径：所有
                .allowedOriginPatterns("*") // 允许跨域访问的源 (Spring Boot 2.4+ 用这个更稳)
                .allowedMethods("POST", "GET", "PUT", "OPTIONS", "DELETE") // 允许的方法
                .allowedHeaders("*") // 允许的头部
                .allowCredentials(true) // 是否允许携带 Cookie
                .maxAge(3600); // 预检请求（OPTIONS）的缓存时间，单位秒
    }
}