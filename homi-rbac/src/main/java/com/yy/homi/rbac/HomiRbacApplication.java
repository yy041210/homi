package com.yy.homi.rbac;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@EnableDiscoveryClient
@MapperScan(basePackages = {"com.yy.homi.rbac.mapper" })
// 排除掉 OAuth2 自动触发的 WebSecurity 配置，防止它拉起 WebSecurityConfigurerAdapter
@SpringBootApplication(scanBasePackages = {"com.yy.homi.rbac","com.yy.homi.common"})
public class HomiRbacApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomiRbacApplication.class,args);
    }
}
