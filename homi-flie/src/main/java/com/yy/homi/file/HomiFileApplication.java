package com.yy.homi.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@MapperScan("com.yy.homi.file.mapper")
@SpringBootApplication(scanBasePackages = {"com.yy.homi.file", "com.yy.homi.common"})
public class HomiFileApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomiFileApplication.class, args);
    }
}
