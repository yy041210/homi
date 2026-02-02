package com.yy.homi.file.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.domain.dto.request.UploadChunkReqDTO;
import com.yy.homi.file.service.SysChunkFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;

@Tag(name = "03.分片处理接口", description = "处理分片的上传、合并操作")
@RestController
@RequestMapping("/syschunkfile")
public class SysChunkFileController {

    @Autowired
    private SysChunkFileService sysChunkFileService;

    //上传分片
    @Operation(summary = "上传分片数据", description = "上传单个分片块，建议前端在任务初始化后并发或顺序调用")
    @PostMapping("/uploadChunk")
    public R uploadChunk(UploadChunkReqDTO uploadChunkReqDTO){
        return  sysChunkFileService.uploadChunk(uploadChunkReqDTO);
    }

    //检查分片
    @Operation(summary = "检查分片状态 (断点续传)", description = "检查某个分片是否已存在，用于实现断点续传")
    @Parameters({
            @Parameter(name = "taskId", description = "任务ID", required = true),
            @Parameter(name = "chunkIndex", description = "分片索引", required = true, example = "1")
    })
    @GetMapping("/checkChunk")
    public R checkChunk(@RequestParam("taskId") @NotNull Long taskId, @RequestParam("chunkIndex") int chunkIndex){
        return sysChunkFileService.checkChunk(taskId,chunkIndex);
    }

    //合并分片
    @Operation(summary = "合并所有分片", description = "当所有分片上传完成后，调用此接口通知后端将分片合成完整文件")
    @Parameter(name = "taskId", description = "任务ID", required = true)
    @GetMapping("/mergeChunk")
    public R mergeChunk(@RequestParam("taskId") @NotNull Long taskId){
        return sysChunkFileService.mergeChunk(taskId);
    }


}
