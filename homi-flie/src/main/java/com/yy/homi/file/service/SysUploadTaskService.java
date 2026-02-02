package com.yy.homi.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.entity.SysUploadTask;

public interface SysUploadTaskService extends IService<SysUploadTask> {
    //初始化 分片上传任务
    R initChunkUploadTask(String fileName, String fileHash, Long totalSize, int totalChunks);
}
