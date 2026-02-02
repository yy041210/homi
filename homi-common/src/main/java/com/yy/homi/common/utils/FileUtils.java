package com.yy.homi.common.utils;


import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * 文件工具类
 */
@Slf4j
public class FileUtils {

    /**
     * 计算输入流的 SHA-256 哈希值
     * * @param is 文件输入流
     *
     * @return 64位十六进制哈希字符串
     */
    public static String getSha256(InputStream is) {
        try {
            // Hutool 的 DigestUtil 会自动处理流的关闭和缓冲区，防止 OOM
            return DigestUtil.sha256Hex(new BufferedInputStream(is));
        } catch (Exception e) {
            log.error("计算文件SHA-256失败", e);
            throw new RuntimeException("文件校验值计算异常");
        }
    }

    /**
     * 根据哈希值生成分片存储路径
     */
    public static String getHashPath(String hash) {
        if (hash == null || hash.length() < 2) {
            return hash;
        }
        return hash.substring(0, 1) + "/" + hash.substring(1, 2) + "/" + hash;
    }

    /**
     * 根据文件名或后缀获取 Content-Type
     *
     * @param filename 文件名 (例如: "test.jpg") 或 后缀 (例如: ".jpg")
     * @return 对应的 MIME 类型，找不到则返回默认的流类型
     */
    public static String getContentType(String filename) {
        return MediaTypeFactory.getMediaType(filename)
                .map(MediaType::toString)
                .orElse("application/octet-stream"); // 默认二进制流
    }

    public static String getChunkFolderPath(String hash) {
        if (hash == null || hash.length() < 2) {
            return hash;
        }
        return hash.substring(0, 1) + "/" + hash.substring(1, 2) + "/" + hash + "/chunk";
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "unkonwn"; // 或者根据业务返回 null
        }
        // 截取最后一个点之后的内容，并转为小写以方便后续匹配桶名
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}