package com.yy.homi.file.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.dto.response.UploadBatchRespDTO;
import com.yy.homi.file.domain.entity.SysFile;
import com.yy.homi.file.domain.vo.SysFileVO;
import com.yy.homi.file.mapper.SysFileMapper;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.file.service.SysFileService;
import com.yy.homi.file.utils.FileTypeUtils;
import com.yy.homi.common.utils.FileUtils;
import io.minio.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    @Autowired
    private SysFileMapper sysFileMapper;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private FileTypeUtils fileTypeUtils;
    @Autowired
    @Lazy
    private SysFileService sysFileService;
    @Resource(name = "homiExecutor")
    private ExecutorService homiExecutor;

    @Override
    @Transactional
    public R uploadOne(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new ServiceException("文件为空");
            }

            //1.将文件流读进内存
            byte[] bytes = file.getBytes();
            String originalFilename = file.getOriginalFilename();
            String extension = FileUtils.getFileExtension(originalFilename);
            //2.计算sha 256值,计算存储路径
            String fileHash = FileUtils.getSha256(new ByteArrayInputStream(bytes));

            //3.秒传检查
            SysFile sysFile = this.getReusableSysFile(fileHash, originalFilename);
            if (sysFile != null) {
                return R.ok(sysFile);
            }

            //4.如果没查询到就保存到minio和文件表中
            SysFile saveSysFile = new SysFile();

            String bucketName = fileTypeUtils.getBucketByExtension(extension);
            String endpoint = fileTypeUtils.getMinioConfig().getEndpoint();
            String objectName = FileUtils.getHashPath(fileHash) + "."+extension;
            String url = endpoint + "/" + bucketName + "/" + objectName;
            saveSysFile.setFileName(originalFilename);
            saveSysFile.setFileHash(fileHash);
            saveSysFile.setExtension(extension);
            saveSysFile.setSize(file.getSize());
            saveSysFile.setBucketName(bucketName);
            saveSysFile.setObjectName(objectName);
            saveSysFile.setUrl(url);
            saveSysFile.setDelFlag(CommonConstants.DEL_NORMAL);
            sysFileMapper.insert(saveSysFile);

            //5.先插入数据库在保存到minio中，这样minio报错，数据库回滚
            this.uploadMinio(bytes, fileHash, saveSysFile.getBucketName(), extension, objectName);

            return R.ok(saveSysFile);
        } catch (IOException e) {
            throw new ServiceException("文件不存在");
        }
    }


    @Override
    @Transactional
    public R deleteOne(String fileId) {
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null) {
            return R.ok("未找到对应的文件");
        }

        //先删除数据库再删除minio防止minio删除后，数据库操作回滚了
        LambdaUpdateWrapper<SysFile> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysFile::getId, fileId)
                .set(SysFile::getDelFlag, CommonConstants.DEL_DELETE); //设置为已删除
        sysFileMapper.update(null, updateWrapper);

        //删除minio中的文件
        SysFile otherUsed = sysFileMapper.findOneByFileHash(sysFile.getFileHash());
        if (otherUsed == null) {
            //没有别的用户使用了，再删除minio中的文件
            String bucketName = sysFile.getBucketName();
            String objectName = sysFile.getObjectName();

            try {
                minioClient.removeObject(
                        RemoveObjectArgs
                                .builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .build()
                );
            } catch (Exception e) {
                throw new ServiceException("minio删除文件失败");
            }
        }

        return R.ok();
    }

    @Override
    public R uploadBatch(MultipartFile[] files) {
        if (files == null || files.length <= 0) {
            return R.fail("请选择需要上传的文件");
        }

        //1.去除重复文件
        List<MultipartFile> fileList = Arrays.stream(files).collect(Collectors.toMap(
                f -> {
                    try (InputStream is = f.getInputStream()) {
                        return f.getOriginalFilename() + "_" + FileUtils.getSha256(is);
                    } catch (IOException e) {
                        log.error("文件不存在", e);
                        throw new ServiceException("文件不存在");
                    }
                },
                f -> f,
                (existing, replacement) -> existing
        )).values().stream().collect(Collectors.toList());

        //2.提交线程池处理
        List<CompletableFuture<R>> futures = fileList.stream().map(file -> {
                    return CompletableFuture
                            .supplyAsync(
                                    () -> {
                                        //使用代理对象调用防止事务注解失效
                                        R r = sysFileService.uploadOne(file);
                                        return r;
                                    }
                                    , homiExecutor)
                            .handle(
                                    (r, ex) -> {
                                        //异常为空就正常返回
                                        if (ex != null) {
                                            String filename = file.getOriginalFilename();
                                            log.error("文件 [" + filename + "] 上传失败", ex);
                                            return R.fail("文件[" + filename + "]上传失败：" + ex.getCause().getMessage());
                                        }
                                        return r;
                                    }
                            );
                })
                .collect(Collectors.toList());

        //3.解析线程处理结果
        List<R> results = futures.stream()
                .map(future -> {
                    return future.join();
                }).collect(Collectors.toList());

        //4.成功的结果
        List<SysFileVO> successFiles = new ArrayList<>();
        List<String> errorMsgs = new ArrayList<>();
        results.stream()
                .filter(r -> r.getCode() ==  HttpStatus.OK.value())
                .forEach(r -> {
                    SysFile data = (SysFile) r.getData();
                    SysFileVO sysFileVO = new SysFileVO();
                    BeanUtils.copyProperties(data, sysFileVO);
                    successFiles.add(sysFileVO);
                });
        results.stream()
                .filter(r -> r.getCode() !=  HttpStatus.OK.value())
                .forEach(r -> {
                    String errorMsg = r.getMsg();
                    errorMsgs.add(errorMsg);
                });
        UploadBatchRespDTO uploadBatchRespDTO = new UploadBatchRespDTO();
        uploadBatchRespDTO.setSuccessFiles(successFiles);
        uploadBatchRespDTO.setErrorMsgs(errorMsgs);
        //5.返回结果
        return R.ok(uploadBatchRespDTO);
    }



    public void uploadMinio(byte[] bytes, String fileHash,String bucketName, String extension, String fileName) {
        try {

            // 上传到 MinIO 时设置响应头
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Disposition", "inline; filename=\"" +
                    URLEncoder.encode(fileName, "UTF-8") + "\"");

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(FileUtils.getHashPath(fileHash) + "." + extension)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType(FileUtils.getContentType(fileName))
                            .headers(headers)
                            .build()
            );
        } catch (Exception e) {
            log.error("Minio上传文件异常: {}", e);
            throw new ServiceException("存储服务器异常，上传失败"); //抛出异常触发回滚
        }
    }

    /**
     * 检查是否存在可秒传或可复用的物理文件
     * @return 如果可以秒传，返回 SysFile 实体；否则返回 null
     */
    public SysFile getReusableSysFile(String fileHash, String filename) {
        // 1. 查找是否存在相同 Hash 的文件
        // 注意：这里建议查出该 Hash 下所有的记录，或者按 extension 过滤
        List<SysFile> existFiles = sysFileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getFileHash, fileHash));

        if (CollectionUtil.isEmpty(existFiles)) {
            return null;
        }

        // 2. 优先检查：是否存在文件名和后缀都一模一样的记录（完全匹配）
        String extension = FileUtils.getFileExtension(filename);
        for (SysFile f : existFiles) {
            if (f.getFileName().equals(filename) && f.getExtension().equals(extension)) {
                return f;
            }
        }

        // 3. 方案 A 逻辑检查：是否存在后缀一致的记录（物理文件可复用）
        // 只要后缀一致，说明物理上在 MinIO 的同一个桶里已经存过这份数据了
        for (SysFile f : existFiles) {
            if (f.getExtension().equals(extension)) {
                // 物理文件一致，但文件名不同 -> 新增一条数据库记录，指向同一个物理地址
                SysFile newFile = new SysFile();
                BeanUtils.copyProperties(f, newFile, "id", "createTime", "updateTime"); // 拷贝物理信息，排除ID和时间
                newFile.setFileName(filename);
                newFile.setSize(f.getSize());
                newFile.setDelFlag(0);
                sysFileMapper.insert(newFile);
                return newFile;
            }
        }
        // 4. Hash 相同但后缀不同：视为不同物理文件，需返回 null 让后续去创建上传任务
        return null;
    }
}
