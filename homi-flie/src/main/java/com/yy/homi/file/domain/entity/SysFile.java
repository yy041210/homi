package com.yy.homi.file.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件实体类
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@TableName("sys_file")
public class SysFile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID) // 使用雪花算法ID
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String fileName;    // 原始文件名
    /**
     * 文件内容的 SHA-256 值 (用于秒传判断)
     */
    private String fileHash;
    private String objectName;  //文件在桶里的名字
    private String bucketName;    // 存储桶名
    private String url;         // 完整访问地址
    private String extension;   // 后缀名
    private Long size;          // 大小

    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
    private int delFlag;     // 逻辑删除
}