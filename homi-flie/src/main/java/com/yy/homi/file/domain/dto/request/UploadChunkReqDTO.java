package com.yy.homi.file.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Schema(description = "分片上传请求参数")
public class UploadChunkReqDTO {
    @Schema(description = "分片任务ID", example = "172045983745", required = true)
    @NotNull(message = "任务id不能为空")
    private Long taskId;

    @Schema(description = "当前分片索引 (从0开始)", example = "0", required = true)
    @NotNull(message = "分片索引不能为空")
    private Integer chunkIndex;

    @Schema(description = "分片文件二进制数据", required = true)
    @NotNull(message = "分片文件不能为空")
    private MultipartFile chunkFile;

    @Schema(description = "当前分片的 SHA-256 哈希值 (用于校验完整性)", example = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", required = true)
    @NotBlank(message = "分片文件的sha 256 hash不能为空")
    private String chunkHash;

    @Schema(description = "存储桶名称", example = "homi-video", required = true)
    @NotBlank
    private String bucketName;
}
