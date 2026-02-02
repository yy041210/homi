package com.yy.homi.file.domain.dto.response;

import com.yy.homi.file.domain.vo.SysFileVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Schema(description = "批量上传结果响应对象")
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UploadBatchRespDTO {
    @Schema(description = "上传成功的文件信息列表")
    private List<SysFileVO> successFiles;
    @Schema(description = "上传失败的错误消息列表", example = "['fileA.jpg: 尺寸超限', 'fileB.png: 存储异常']")
    private List<String> errorMsgs;
}
