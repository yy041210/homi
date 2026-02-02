package com.yy.homi.file.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 上传任务主表：记录文件整体信息
 */
@Data
@TableName("sys_upload_task")
public class SysUploadTask implements Serializable {

    public static final int TASK_STATUS_UPLOADING = 0; //上传中
    public static final int TASK_STATUS_MERGING = 1; //合并中
    public static final int TASK_STATUS_FINISH = 2; //完成
    public static final int TASK_STATUS_FAIL = -1; //失败


    @TableId(type = IdType.ASSIGN_ID)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    /**
     * 母文件唯一哈希 (SHA-256)
     * 逻辑隔离的核心：作为分片表的 parent_hash
     */
    private String fileHash;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件后缀 (用于后端判断并分配 bucket_name)
     */
    private String extension;

    /**
     * 存储桶名称
     * 在初始化任务时根据 extension 判定，后续分片和合并均使用此桶
     */
    private String bucketName;

    /**
     * 文件总大小 (单位: 字节)
     */
    private Long totalSize;

    /**
     * 总分片数
     */
    private Integer totalChunks;

    /**
     * 任务状态：0-上传中，1-合并中，2-已完成，3-失败
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

}