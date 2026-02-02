package com.yy.homi.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class Base64Utils {

    /**
     * 将 classpath 下的图片转换为 HTML 可用的 Data URL 格式
     * @param imagePath 相对 static 的路径，如 "static/images/logo.png"
     * @return 完整的 data:image/png;base64,xxxx 字符串
     */
    public static String imageToBase64(String imagePath) {
        try {
            ClassPathResource resource = new ClassPathResource(imagePath);
            InputStream inputStream = resource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            
            String base64Content = Base64.getEncoder().encodeToString(bytes);
            
            // 自动识别后缀名（简单处理）
            String extension = imagePath.substring(imagePath.lastIndexOf(".") + 1);
            return "data:image/" + extension + ";base64," + base64Content;
        } catch (Exception e) {
            log.error("图片转换 Base64 失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 普通字符串加密
     */
    public static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 普通字符串解密
     */
    public static String decode(String base64Text) {
        byte[] decodedBytes = Base64.getDecoder().decode(base64Text);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

}