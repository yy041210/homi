package com.yy.homi.file.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SysFileVO implements Serializable {

    private static final long serialVersionUID = 1L;


    private Long id;

    private String fileName;    // 原始文件名、
    private String  objectName;

    private String url;         // 完整访问地址
    private String extension;   // 后缀名
    private Long size;          // 大小

}