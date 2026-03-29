package com.yy.homi.common.utils;

import com.alibaba.fastjson.JSON;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;

public class ServletUtils {

    // 获取注解工具
    public static <T extends Annotation> T getAnnotation(JoinPoint joinPoint, Class<T> cls) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getAnnotation(cls);
    }

    // 参数转 JSON 字符串 (排除文件对象)
    public static String argsArrayToString(Object[] paramsArray) {
        if (paramsArray == null || paramsArray.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (Object obj : paramsArray) {
            if (obj != null && !(obj instanceof MultipartFile) && !(obj instanceof HttpServletRequest)) {
                sb.append(JSON.toJSONString(obj)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 获取客户端真实IP地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (ip != null && ip.length() > 15) {
            if (ip.indexOf(",") > 0) {
                ip = ip.substring(0, ip.indexOf(","));
            }
        }

        // 如果是本地回环地址，根据网卡取本机配置的IP
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }

}
