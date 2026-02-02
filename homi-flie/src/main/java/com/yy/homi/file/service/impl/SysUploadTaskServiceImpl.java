package com.yy.homi.file.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.entity.SysChunkFile;
import com.yy.homi.file.domain.entity.SysFile;
import com.yy.homi.file.domain.entity.SysUploadTask;
import com.yy.homi.file.mapper.SysChunkFileMapper;
import com.yy.homi.file.mapper.SysUploadTaskMapper;
import com.yy.homi.file.service.SysFileService;
import com.yy.homi.file.service.SysUploadTaskService;
import com.yy.homi.file.utils.FileTypeUtils;
import com.yy.homi.common.utils.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SysUploadTaskServiceImpl extends ServiceImpl<SysUploadTaskMapper, SysUploadTask> implements SysUploadTaskService {

    @Autowired
    private SysFileService sysFileService;
    @Autowired
    private SysUploadTaskMapper sysUploadTaskMapper;
    @Autowired
    private SysChunkFileMapper sysChunkFileMapper;
    @Autowired
    private FileTypeUtils fileTypeUtils;

    @Override
    public R initChunkUploadTask(String fileName, String fileHash, Long totalSize, int totalChunks) {
        //1.一：秒传检查
        SysFile sysFile = sysFileService.getReusableSysFile(fileHash, fileName);
        if (sysFile != null) {
            JSONObject jsonObject = (JSONObject) JSON.toJSON(sysFile);
            jsonObject.put("status", "COMPLETED");
            return R.ok(jsonObject);
        }

        //2.二：预检判断是否有正在上传的相同的任务.fileHash + extension锁定任务
        String extension = FileUtils.getFileExtension(fileName);
        SysUploadTask task = sysUploadTaskMapper
                .selectOne(
                        new LambdaQueryWrapper<SysUploadTask>().
                                eq(SysUploadTask::getFileHash, fileHash)
                                .eq(SysUploadTask::getExtension, extension)

                );

        if (task == null) {
            // --- 情况 A：开启全新的上传任务 ---
            String bucketName = fileTypeUtils.getBucketByExtension(extension); // 获取该后缀对应的桶名
            task = new SysUploadTask();
            task.setFileHash(fileHash);
            task.setFileName(fileName);
            task.setBucketName(bucketName);
            task.setExtension(extension);
            task.setTotalSize(totalSize);
            task.setTotalChunks(totalChunks);
            task.setStatus(SysUploadTask.TASK_STATUS_UPLOADING); // 0: 上传中
            this.save(task);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "UPLOADING");
            jsonObject.put("bucketName", bucketName);
            jsonObject.put("finishChunks", Collections.emptyList());
            jsonObject.put("taskId",task.getId());
            return R.ok(jsonObject);
        }

        //3.任务已经存在根据状态处理
        //任务是合并中(前端循环发送请求)
        if (task.getStatus() == SysUploadTask.TASK_STATUS_MERGING) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "MERGING");
            jsonObject.put("msg", "合并时间较长，请重试");
            jsonObject.put("taskId",task.getId());
            return R.ok(jsonObject);
        }

        //4.完成，但是秒传没有查到记录（可能是程序到这才完成，或者sys_file记录被删除了）
        if (task.getStatus() == SysUploadTask.TASK_STATUS_FINISH) {
            //修复sys_file，这里简单处理为：删除这个任务让用户重新传
            sysUploadTaskMapper.deleteById(task.getId());
            return initChunkUploadTask(fileName, fileHash, totalSize, totalChunks);
        }


        //5.失败，合并失败可能是分片被删除了，数据库记录没有删除，删除所有数据库记录重新上传
        if (task.getStatus() == SysUploadTask.TASK_STATUS_FAIL) {
            log.warn("检测到失败任务，执行彻底重置逻辑: taskId=" + task.getId());
            //删除该任务的分片记录，重新上传
            sysChunkFileMapper.delete(new LambdaQueryWrapper<SysChunkFile>().eq(SysChunkFile::getTaskId, task.getId()));
            //修改任务状态为上传
            sysUploadTaskMapper.update(null,new LambdaUpdateWrapper<SysUploadTask>()
                    .eq(SysUploadTask::getId,task.getId())
                    .set(SysUploadTask::getStatus,SysUploadTask.TASK_STATUS_UPLOADING)
            );
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("status", "UPLOADING");
            jsonObject.put("bucketName", task.getBucketName());
            jsonObject.put("finishChunks", Collections.emptyList());
            jsonObject.put("taskId",task.getId());
            return R.ok(jsonObject);
        }

        //6.上传中,就断点续传
        List<SysChunkFile> sysChunkFiles = sysChunkFileMapper
                .selectList(
                        new LambdaQueryWrapper<SysChunkFile>()
                                .eq(SysChunkFile::getTaskId, task.getId())
                );
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("status", "UPLOADING");
        jsonObject.put("bucketName", task.getBucketName());
        jsonObject.put("finishChunks", sysChunkFiles);
        jsonObject.put("taskId",task.getId());
        return R.ok(jsonObject);

    }
}
