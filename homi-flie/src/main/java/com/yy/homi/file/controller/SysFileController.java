package com.yy.homi.file.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.file.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Tag(name = "01.基础文件接口", description = "处理单个或批量文件的标准上传与删除")
@RestController
@RequestMapping("/sysfile")
public class SysFileController {

    @Autowired
    private SysFileService sysFileService;

    //上传单个文件
    @Operation(summary = "上传单个文件", description = "标准 Multipart 格式上传")
    @PostMapping("/uploadOne")
    public R uploadOne(@RequestParam("file") @NotNull MultipartFile file) {
        return sysFileService.uploadOne(file);
    }

    //上传多个文件
    @Operation(summary = "批量上传文件", description = "支持同时选中多个文件上传")
    @PostMapping("/uploadBatch")
    public R uploadBatch(@RequestParam("files") MultipartFile[] files) {
        return sysFileService.uploadBatch(files);
    }

    //根据文件id删除文件
    @Operation(summary = "删除文件", description = "根据文件唯一ID删除存储及数据库记录")
    @Parameter(name = "fileId", description = "文件唯一标识", required = true, example = "771239485")
    @GetMapping("/deleteOne")
    public R deleteOne(@RequestParam("fileId") @NotBlank String fileId) {
        return sysFileService.deleteOne(fileId);
    }


}
