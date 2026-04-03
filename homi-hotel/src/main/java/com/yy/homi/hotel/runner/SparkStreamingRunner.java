package com.yy.homi.hotel.runner;

import com.yy.homi.hotel.task.SparkStreamingTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SparkStreamingRunner implements CommandLineRunner {

    @Autowired
    private SparkStreamingTask SparkStreamingTask;

    @Override
    public void run(String... args) {
        log.info(">>> 正在准备启动 Spark Streaming 实时热度计算任务...");

        // 必须开启新线程！否则 Spark 的 awaitTermination() 会阻塞 Spring Boot 启动过程
        Thread sparkThread = new Thread(() -> {
            try {
                // 调用你写的实时处理逻辑
                SparkStreamingTask.startRealTimeProcessing();
            } catch (Exception e) {
                log.error(">>> Spark 实时任务运行异常: ", e);
            }
        });

        // 设置为守护线程，确保主程序关闭时 Spark 也能优雅退出
        sparkThread.setDaemon(true);
        sparkThread.setName("Top10HotelWeight-Streaming-Worker");
        sparkThread.start();
        
        log.info(">>> Spark 实时任务已在后台线程启动。");
    }
}