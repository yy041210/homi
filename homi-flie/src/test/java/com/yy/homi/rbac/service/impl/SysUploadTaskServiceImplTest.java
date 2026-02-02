package com.yy.homi.rbac.service.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.service.SysUploadTaskService;
import com.yy.homi.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test; // 注意：JUnit5 使用这个注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@SpringBootTest // 该注解会启动整个 Spring 上下文，以便注入 Service 和 Mapper
public class SysUploadTaskServiceImplTest {

    @Autowired
    private SysUploadTaskService sysUploadTaskService;

    // 测试配置文件路径
    private final String filePath = "D:\\BaiduNetdiskDownload\\license plate recognition.zip";
    private final String bucketName = "homi-file";
    private final Long chunkSize = 1024 * 1024 * 20L;  //分块大小

    @Test
    public void testInitTaskConcurrency() throws InterruptedException, ExecutionException {

        File file = new File(filePath);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            log.error("未找到文件");
        }
        String fileName = file.getName();
        Long totalSize = file.length();
        String fileHash = FileUtils.getSha256(fis);

        // 注意：必须先将其中一个变量转为 double，否则整数除法会先丢失精度
        int totalChunks = (int) Math.ceil((double) totalSize / chunkSize);

        // 1. 模拟 100 个线程同时调用接口
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // CountDownLatch 像发令枪，让 50 个线程在同一时刻“起跑”
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<R>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Future<R> future = executorService.submit(() -> {
                try {
                    latch.await(); // 所有线程在此等待指令
                    return sysUploadTaskService.initChunkUploadTask(fileName, fileHash, totalSize, totalChunks);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            });
            futures.add(future);
        }

        System.out.println("准备就绪，开始高并发冲击...");
        latch.countDown(); //所有线程同时开始执行

        // 2. 统计结果
        int successCount = 0;
        for (Future<R> future : futures) {
            R result = future.get();
            if (result != null && result.getCode() == 200) {
                successCount++;
            }
        }

        System.out.println("测试完成！");
        System.out.println("并发请求数: " + threadCount);
        System.out.println("成功获得 R.ok 的请求数: " + successCount);

        executorService.shutdown();
    }
}