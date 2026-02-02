package com.yy.homi.file.utils;


import com.yy.homi.file.config.MinioConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@Component
public class FileTypeUtils {

    @Autowired
    private MinioConfig minioConfig;

    /**
     * 根据后缀名获取对应的桶名
     */
    public String getBucketByExtension(String ext) {
        if (ext == null) return minioConfig.getBucketDefault();
        
        ext = ext.toLowerCase();
        // 图片类
        if (ext.matches("(jpg|jpeg|png|gif|bmp|webp)")) {
            return minioConfig.getBucketImage();
        }
        // 视频类
        if (ext.matches("(mp4|avi|mkv|flv|wmv)")) {
            return minioConfig.getBucketVideo();
        }
        // 默认桶
        return minioConfig.getBucketDefault();
    }
}