package com.yy.homi.file.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.codec.JsonJacksonCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private String port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:3000}")
    private int timeout;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 1. 组装地址（Redisson 强制要求 redis:// 前缀）
        String address = host.startsWith("redis://") ? host : "redis://" + host + ":" + port;
        
        // 2. 单机模式配置
        config.useSingleServer()
              .setAddress(address)
              .setDatabase(database)
              .setTimeout(timeout)
              .setConnectionMinimumIdleSize(8)  // 最小空闲连接
              .setConnectionPoolSize(64)       // 最大连接数
              .setIdleConnectionTimeout(10000) // 连接空闲多久被释放
              .setConnectTimeout(10000)        // 建立连接超时
              .setRetryAttempts(3)             // 命令执行重试次数
              .setRetryInterval(1500);         // 命令重试间隔

        // 3. 设置密码（非空才设置）
        if (StringUtils.hasText(password)) {
            config.useSingleServer().setPassword(password);
        }

        // 4. 设置序列化方式（默认使用 Jackson 序列化，建议显式声明）
        config.setCodec(new JsonJacksonCodec());

        return Redisson.create(config);
    }
}