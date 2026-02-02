package com.yy.homi.file.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.config.MinioConfig;
import com.yy.homi.file.domain.dto.request.UploadChunkReqDTO;
import com.yy.homi.file.domain.entity.SysChunkFile;
import com.yy.homi.file.domain.entity.SysFile;
import com.yy.homi.file.domain.entity.SysUploadTask;
import com.yy.homi.file.mapper.SysChunkFileMapper;
import com.yy.homi.file.mapper.SysFileMapper;
import com.yy.homi.file.mapper.SysUploadTaskMapper;
import com.yy.homi.file.constant.FileConstants;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.file.service.SysChunkFileService;
import com.yy.homi.common.utils.FileUtils;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Service
public class SysChunkFileServiceImpl extends ServiceImpl<SysChunkFileMapper, SysChunkFile> implements SysChunkFileService {

    @Autowired
    private SysChunkFileMapper sysChunkFileMapper;
    @Autowired
    private SysFileMapper sysFileMapper;
    @Autowired
    private SysUploadTaskMapper sysUploadTaskMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private MinioConfig minioConfig;
    @Resource(name = "homiExecutor")
    private ExecutorService executorService;


    //前提条件：数据库
    @Override
    public R uploadChunk(UploadChunkReqDTO uploadChunkReqDTO) {
        //1.准备变量
        Long taskId = uploadChunkReqDTO.getTaskId();
        Integer chunkIndex = uploadChunkReqDTO.getChunkIndex();
        String lockKey = String.format(FileConstants.UPLOAD_CHUNK_LOCK_PREFIX, taskId, chunkIndex);
        RLock lock = redissonClient.getLock(lockKey);

        //2.检验上传分片是否完整
        MultipartFile chunkFile = uploadChunkReqDTO.getChunkFile();
        String correctChunkHash = uploadChunkReqDTO.getChunkHash(); //前端传来正确的文件 sha256 hash

        byte[] bytes = new byte[0];
        try {
            bytes = chunkFile.getBytes();
        } catch (IOException e) {
            throw new ServiceException("文件不存在！异常原因：" + e.getMessage());
        }
        long size = chunkFile.getSize();
        String chunkHash = FileUtils.getSha256(new ByteArrayInputStream(bytes));
        if (!chunkHash.equals(correctChunkHash)) {
            return R.fail("分片不完整！请重新上传");
        }

        //3.获取上传分片资格
        if (lock.tryLock()) {
            try {
                //4.上传到minio
                String chunkObjectName = String.format(FileConstants.CHUNK_MINIO_PATH, taskId, chunkIndex);
                String bucketName = uploadChunkReqDTO.getBucketName();

                PutObjectArgs putObjectArgs = PutObjectArgs
                        .builder()
                        .stream(new ByteArrayInputStream(bytes), size, -1)
                        .bucket(bucketName)
                        .object(chunkObjectName)
                        .build();
                minioClient.putObject(putObjectArgs);

                //5.先插入数据库，根据数据库 taskId＋chunkIndex唯一索引 确保只有一条数据能够插入成功
                SysChunkFile sysChunkFile = new SysChunkFile();
                sysChunkFile.setTaskId(taskId);
                sysChunkFile.setChunkIndex(chunkIndex);
                sysChunkFile.setChunkHash(chunkHash);
                sysChunkFile.setSize(size);
                sysChunkFile.setChunkObjectName(chunkObjectName);
                sysChunkFile.setBucketName(bucketName);
                sysChunkFileMapper.insert(sysChunkFile);
                return R.ok("上传成功");
            } catch (DuplicateKeyException e) {
                log.warn("并发冲突，分片已由其他请求完成: taskId=" + taskId + ", index=" + chunkIndex);
                return R.ok("上传成功");
            } catch (Exception e) {
                throw new ServiceException("minio上传失败！异常信息：" + e.getMessage());
            } finally {
                lock.unlock();
            }
        } else {
            return R.fail("系统繁忙，请重试!");
        }

    }


    @Override
    public R mergeChunk(Long taskId) {
        RLock lock = redissonClient.getLock(String.format(FileConstants.MERGE_CHUNK_LOCK_PREFIX, taskId));

        try {
            if (lock.tryLock()) {
                //1.查询分片任务
                SysUploadTask sysUploadTask = sysUploadTaskMapper.selectById(taskId);
                if (sysUploadTask == null) {
                    return R.fail("没有该任务");
                }

                String fileHash = sysUploadTask.getFileHash();
                String objectName = FileUtils.getHashPath(fileHash) + "." + sysUploadTask.getExtension();
                String bucketName = sysUploadTask.getBucketName();

                //2.被人家合并完了就直接返回
                if (sysUploadTask.getStatus() == SysUploadTask.TASK_STATUS_FINISH) {
                    JSONObject jsonObject = new JSONObject();
                    String url = minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName;
                    jsonObject.put("objetName", objectName);
                    jsonObject.put("url", url);
                    return R.ok(jsonObject);
                }

                //3.查询已上传的分片列表
                List<SysChunkFile> sysChunkFiles = sysChunkFileMapper
                        .selectList(
                                new LambdaQueryWrapper<SysChunkFile>()
                                        .eq(SysChunkFile::getTaskId, taskId)
                                        .orderByAsc(SysChunkFile::getChunkIndex)
                        );
                if (sysChunkFiles.size() < sysUploadTask.getTotalChunks()) {
                    return R.fail("分片尚未全部上传完成！");
                }

                List<ComposeSource> sourceObjectList = new ArrayList<>();
                sysChunkFiles
                        .stream()
                        .forEach(chunk -> {
                            sourceObjectList.add(ComposeSource.builder()
                                    .bucket(bucketName)
                                    .object(chunk.getChunkObjectName())
                                    .build());
                        });
                //4.执行minio合并
                // 合并上传到 MinIO 时设置响应头
                try {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", FileUtils.getContentType(objectName));
                    String encodedName = URLEncoder.encode(objectName, "UTF-8").replaceAll("\\+", "%20");
                    headers.put("Content-Disposition", "inline; filename=\"" + encodedName + "\"; filename*=utf-8''" + encodedName);
                    minioClient.composeObject(
                            ComposeObjectArgs
                                    .builder()
                                    .bucket(bucketName)
                                    .object(objectName)
                                    .extraHeaders(headers)  // 使用 extraHeaders
                                    .sources(sourceObjectList)
                                    .build()
                    );
                } catch (Exception e) {
                    log.error("合并分片出错,将任务 " + taskId + " 状态改为FAIL！异常信息：" + e.getMessage());
                    sysUploadTaskMapper.update(null,
                            new LambdaUpdateWrapper<SysUploadTask>()
                                    .eq(SysUploadTask::getId, taskId)
                                    .set(SysUploadTask::getStatus, SysUploadTask.TASK_STATUS_FAIL)
                    );
                    return R.fail("合并分片出错");
                }

                //5.修改分片任务的状态
                sysUploadTaskMapper.update(null,
                        new LambdaUpdateWrapper<SysUploadTask>()
                                .eq(SysUploadTask::getId, taskId)
                                .set(SysUploadTask::getStatus, SysUploadTask.TASK_STATUS_FINISH)
                );

                //6.插入sys_file记录
                SysFile sysFile = new SysFile();
                sysFile.setFileName(sysUploadTask.getFileName());
                sysFile.setFileHash(fileHash);
                sysFile.setObjectName(objectName);
                sysFile.setBucketName(bucketName);
                sysFile.setSize(sysUploadTask.getTotalSize());
                sysFile.setExtension(sysUploadTask.getExtension());
                sysFile.setUrl(minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName);
                sysFileMapper.insert(sysFile);

                //7.删除数据库分片记录
                List<Long> chunkIds = sysChunkFiles.stream().map(SysChunkFile::getId).collect(Collectors.toList());
                sysChunkFileMapper.deleteBatchIds(chunkIds);

                //8.异步处理minio的分片记录
                CompletableFuture.runAsync(() -> {
                    //构造批量删除列表
                    List<DeleteObject> deleteObjects = sysChunkFiles.stream()
                            .map(chunk -> new DeleteObject(chunk.getChunkObjectName()))
                            .collect(Collectors.toList());
                    //执行批量删除
                    Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                            RemoveObjectsArgs.builder()
                                    .bucket(bucketName)
                                    .objects(deleteObjects)
                                    .build()
                    );

                    //必须遍历一遍结果才会被删除
                    results.forEach(result -> {
                        DeleteError deleteError = null;
                        try {
                            deleteError = result.get();
                        } catch (Exception e) {
                            log.error("遍历MinIO删除结果时发生异常！异常信息：" + e.getMessage());
                        }
                        if (deleteError != null) {
                            log.error("桶：" + deleteError.bucketName() + "，分片" + deleteError.objectName() + "删除失败!" + "失败原因：" + deleteError.message());
                        }
                    });

                }, executorService);


                String url = minioConfig.getEndpoint() + "/" + bucketName + "/" + objectName;
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("objetName", objectName);
                jsonObject.put("url", url);
                return R.ok(jsonObject);

            } else {
                return R.fail("合并任务处理中，请稍后再试");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public R checkChunk(Long taskId, int chunkIndex) {
        // 1. 先查数据库
        SysChunkFile sysChunkFile = sysChunkFileMapper
                .selectOne(
                        new LambdaQueryWrapper<SysChunkFile>()
                                .eq(SysChunkFile::getTaskId, taskId)
                                .eq(SysChunkFile::getChunkIndex, chunkIndex)
                );
        if (sysChunkFile == null) {
            return R.ok(false);
        }

        // 2. 数据库有记录，去 MinIO 真正核实物理文件是否存在
        String bucketName = sysChunkFile.getBucketName();
        String chunkObjetName = sysChunkFile.getChunkObjectName();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(chunkObjetName)
                            .build()
            );
            return R.ok(true);
        } catch (ErrorResponseException e) {
            // 判断是否为文件不存在的错误码
            String errorCode = e.errorResponse().code();
            if ("NoSuchKey".equals(errorCode) || "NoSuchBucket".equals(errorCode)) {
                log.warn("分片数据库记录存在但物理文件缺失: taskId=" + taskId + ", chunkIndex=" + chunkIndex);
                return R.ok(false);
            }
            throw new ServiceException("MinIO通信异常！异常信息：" + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException("核实分片状态失败！taskId=" + taskId + "，异常信息：" + e.getMessage());
        }

    }
}
