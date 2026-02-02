package com.yy.homi.gateway;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

@EnableDiscoveryClient
@SpringBootApplication(
        scanBasePackages = {"com.yy.homi.gateway", "com.yy.homi.common.domain","com.yy.homi.common.constant"},
        exclude = {
                DataSourceAutoConfiguration.class,
                DataSourceTransactionManagerAutoConfiguration.class,
                MybatisPlusAutoConfiguration.class
        }) //common中有
public class HomiGatewayApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HomiGatewayApplication.class, args);
        // 打印所有配置源名字
        context.getEnvironment().getPropertySources().forEach(ps -> System.out.println("已加载配置源: " + ps.getName()));
    }
}
