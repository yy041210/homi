package com.yy.homi.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.dto.request.UploadChunkReqDTO;
import com.yy.homi.file.domain.entity.SysChunkFile;

public interface SysChunkFileService extends IService<SysChunkFile> {
    R uploadChunk(UploadChunkReqDTO uploadChunkReqDTO);

    R mergeChunk(Long taskId);

    R checkChunk(Long taskId, int chunkIndex);
}
