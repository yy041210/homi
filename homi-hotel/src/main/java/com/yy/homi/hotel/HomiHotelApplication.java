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
        // 打印所有相关的 Bean
        String[] beanNames = context.getBeanDefinitionNames();
        System.out.println("=== UserActionLog 相关 Bean ===");
        Arrays.stream(beanNames)
                .filter(name -> name.toLowerCase().contains("useractionlog"))
                .forEach(System.out::println);

        // 检查 Controller 是否被管理
        try {
            Object controller = context.getBean("userActionLogController");
            System.out.println("Controller Bean found: " + controller);
        } catch (Exception e) {
            System.out.println("Controller Bean NOT found!");
        }

        // 检查 Service 是否被管理
        try {
            Object service = context.getBean("userActionLogService");
            System.out.println("Service Bean found: " + service);
        } catch (Exception e) {
            System.out.println("Service Bean NOT found!");
        }
    }

}