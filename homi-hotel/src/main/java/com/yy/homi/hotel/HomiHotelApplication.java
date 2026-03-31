package com.yy.homi.hotel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

@EnableFeignClients
@MapperScan("com.yy.homi.hotel.mapper")
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.yy.homi.hotel", "com.yy.homi.common"})
public class HomiHotelApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(HomiHotelApplication.class, args);
    }

}