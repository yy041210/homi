package com.yy.homi.thirdparty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.yy.homi.thirdparty", "com.yy.homi.common"})
public class HomiThirdPartyApplication {
    public static void main(String[] args) {
        SpringApplication.run(HomiThirdPartyApplication.class,args);
    }
}
