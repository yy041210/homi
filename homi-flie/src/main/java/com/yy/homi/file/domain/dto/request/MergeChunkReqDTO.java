package com.yy.homi.file.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "合并分片请求参数")
public class MergeChunkReqDTO {
    @Schema(description = "上传任务ID", example = "172045983745")
    private String taskId;

    @Schema(description = "原始文件名", example = "presentation.mp4")
    private String fileName;

    @Schema(description = "桶名", example = "homi-video")
    private String bucketName;

    @Schema(description = "总分片数量 (用于最终校验)", example = "12")
    private Integer totalChunks;
}