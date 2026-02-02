package com.yy.homi.file.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.service.SysUploadTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Tag(name = "02.大文件上传任务", description = "大文件分片上传的任务初始化管理")
@RestController
@RequestMapping("/sysuploadtask")
public class SysUploadTaskController {

    @Autowired
    private SysUploadTaskService sysUploadTaskService;

    //初始化 分片上传任务
    @Operation(summary = "初始化分片上传任务", description = "在上传大文件前调用，获取任务ID并检查是否可以秒传")
    @Parameters({
            @Parameter(name = "fileName", description = "原始文件名", required = true, example = "movie.mp4"),
            @Parameter(name = "fileHash", description = "整个文件的 MD5/SHA256 值", required = true, example = "abc123hash"),
            @Parameter(name = "totalSize", description = "文件总大小 (字节)", required = true, example = "104857600"),
            @Parameter(name = "totalChunks", description = "计算出的总分片数", required = true, example = "20")
    })
    @GetMapping("/initchunkuploadtask")
    public R initChunkUploadTask(
            @RequestParam("fileName") @NotBlank String fileName,
            @RequestParam("fileHash") @NotBlank String fileHash,
            @RequestParam("totalSize") @NotNull Long totalSize,
            @RequestParam("totalChunks") int totalChunks
    ){
        return sysUploadTaskService.initChunkUploadTask(fileName,fileHash,totalSize,totalChunks);
    }
}
