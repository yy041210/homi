package com.yy.homi.rbac.service.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.entity.SysUploadTask;
import com.yy.homi.file.domain.dto.request.UploadChunkReqDTO;
import com.yy.homi.file.mapper.SysUploadTaskMapper;
import com.yy.homi.file.service.SysChunkFileService;
import com.yy.homi.common.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@SpringBootTest
public class SysChunkFileServiceImplTest {

    @Autowired
    private SysChunkFileService sysChunkFileService;
    @Autowired
    private SysUploadTaskMapper sysUploadTaskMapper;

    // 测试配置文件路径
    private final String filePath = "D:\\BaiduNetdiskDownload\\license plate recognition.zip";
    private final String bucketName = "homi-file";
    private final Long chunkSize = 1024 * 1024 * 20L;  //分块大小
    private final Long taskId = 2013429648273022979L;

    @Test
    @DisplayName("高并发上传测试：模拟多线程同时上传同一个分片")
    public void testConcurrentUpload() throws Exception {
        int threadCount = 50; // 50个请求同时抢传第1片
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        // 1. 从大文件中切出第一片作为素材
        byte[] firstChunkData = getChunkDataFromFile(filePath, 0, chunkSize);
        String chunkHash = FileUtils.getSha256(new ByteArrayInputStream(firstChunkData));

        List<CompletableFuture<R>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await(); // 等待发令枪响
                    MockMultipartFile file = new MockMultipartFile("chunkFile", "part_1", "application/octet-stream", firstChunkData);

                    UploadChunkReqDTO dto = new UploadChunkReqDTO();
                    dto.setTaskId(taskId);
                    dto.setChunkIndex(1); // 固定上传第1片
                    dto.setChunkFile(file);
                    dto.setChunkHash(chunkHash);
                    dto.setBucketName(bucketName);

                    return sysChunkFileService.uploadChunk(dto);
                } catch (Exception e) {
                    return R.fail(e.getMessage());
                }
            }, executor));
        }

        System.out.println("🚀 准备就绪，50个请求开始冲撞 uploadChunk 接口...");
        latch.countDown();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 2. 验证结果
        int successCount = 0;
        int failCount = 0;
        for (CompletableFuture<R> f : futures) {
            R r = f.join();
            if (r.getCode() == 200) {
                successCount++;
            } else if (r.getCode() == 500) {
                failCount++;
                log.error("异常信息：{}", r.getMsg());
            }
        }
        System.out.println("✅ 结果：并发请求50次，r.ok次数：" + successCount);
    }


    @Test
    @DisplayName("高并发上传测试：模拟多线程同时上传同一个大文件")
    public void testConcurrentUploadBigFile() throws Exception {
        int threadCount = 10; // 40个请求同时抢传一个分片
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        //1.大文件需要分成的片数
        SysUploadTask sysUploadTask = sysUploadTaskMapper.selectById(taskId);
        Integer totalChunks = sysUploadTask.getTotalChunks();

        for (int i = 0; i < totalChunks; i++) {
            System.out.println("=======================  分片" + (i+1) + "==========================");
            byte[] firstChunkData = getChunkDataFromFile(filePath, i, chunkSize);
            String chunkHash = FileUtils.getSha256(new ByteArrayInputStream(firstChunkData));
            List<CompletableFuture<R>> futures = new ArrayList<>();

            for (int j = 0; j < threadCount; j++) {
                int finalI = i+1;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        latch.await(); // 等待发令枪响
                        MockMultipartFile chunkFile = new MockMultipartFile("chunkFile", "part_1", "application/octet-stream", firstChunkData);

                        UploadChunkReqDTO dto = new UploadChunkReqDTO();
                        dto.setTaskId(taskId);
                        dto.setChunkIndex(finalI); // 固定上传第1片
                        dto.setChunkFile(chunkFile);
                        dto.setChunkHash(chunkHash);
                        dto.setBucketName(bucketName);

                        boolean flag = true;
                        R r = null;
                        while (flag) {
                            Thread.sleep(1000);
                            r = sysChunkFileService.checkChunk(taskId, finalI);
                            if (!(boolean) r.getData()) {
                                r = sysChunkFileService.uploadChunk(dto);
                                if (r.getCode() != 200) {
                                    System.out.println("第" + finalI + "个分片上传返回fail！原因：" + r.getMsg());
                                } else {
                                    flag = false;
                                }
                            } else {
                                flag = false;
                            }
                        }
                        return r;
                    } catch (Exception e) {
                        return R.fail(e.getMessage());
                    }
                }, executor));
            }

            System.out.println("🚀 准备就绪，40个请求开始冲撞 uploadChunk 接口...");
            latch.countDown();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            System.out.println("=======================   结束   ==========================");
        }

    }

    @Test
    @DisplayName("高并发合并测试：模拟分片完成后疯狂点击合并")
    public void testConcurrentMerge() throws Exception {
        // 前提：确保分片已经通过上面的方法或工具全部传完
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        List<CompletableFuture<R>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    latch.await();
                    return sysChunkFileService.mergeChunk(taskId);
                } catch (Exception e) {
                    return R.fail(e.getMessage());
                }
            }, executor));
        }

        System.out.println("🚀 准备就绪，10个线程同时发起 mergeChunk 请求...");
        latch.countDown();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 验证分布式锁是否生效
        AtomicInteger mergedSuccess = new AtomicInteger();
        AtomicInteger blockedByLock = new AtomicInteger();

        futures.forEach(f -> {
            R r = f.join();
            if (r.getCode() == 200) mergedSuccess.getAndIncrement();
            else if (r.getMsg().contains("处理中")) blockedByLock.getAndIncrement();
        });

        System.out.println("🏆 合并逻辑进入次数：" + mergedSuccess.get());
        System.out.println("🔒 被分布式锁拦截次数：" + blockedByLock.get());
    }

    /**
     * 工具方法：从指定大文件中读取分片数据
     */
    private byte[] getChunkDataFromFile(String filePath, int index, long chunkSize) throws IOException {
        File file = new File(filePath);
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long offset = index * chunkSize;
            raf.seek(offset);
            int length = (int) Math.min(chunkSize, file.length() - offset);
            byte[] b = new byte[length];
            raf.readFully(b);
            return b;
        }
    }
}