package com.yy.homi.common.config;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

@Configuration
@EnableAsync // 必须开启异步支持
public class ThreadPoolConfig {
    //指定shutdown注销方法，防止spring容器关闭暴力关闭线程，导致任务未执行完
    @Bean(value = "homiExecutor",destroyMethod = "shutdown")
    public ExecutorService homiExecutor(){
        return new ThreadPoolExecutor(
              8,
              16,
              60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200),
                ThreadFactoryBuilder.create().setNamePrefix("homi-pool-").setDaemon(false).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
