package com.yy.homi.common.utils;

import cn.hutool.core.util.IdUtil;

public class IdUtils {
    /**
     * 获取随机UUID（无横线）
     */
    public static String fastSimpleUUID() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 获取雪花算法ID (分布式环境唯一ID)
     */
    public static long getSnowflakeId() {
        return IdUtil.getSnowflakeNextId();
    }
}