package com.yy.homi.file.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 分片明细表：记录每个物理分片的信息
 */
@Data
@TableName("sys_chunk_file")
public class SysChunkFile implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long taskId;

    private String bucketName;

    /**
     * 分片索引 (第几个分片，从1开始)
     */
    private int chunkIndex;

    /**
     * 分片自身哈希 (可选，用于校验单片完整性)
     */
    private String chunkHash;

    /**
     * 物理存储路径 
     * 格式示例：a/b/ab1341431/chunk-1.part
     */
    private String chunkObjectName;

    /**
     * 当前分片大小
     */
    private Long size;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

}