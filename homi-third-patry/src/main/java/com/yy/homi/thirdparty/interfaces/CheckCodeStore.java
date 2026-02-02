package com.yy.homi.thirdparty.interfaces;

/**
 * @description 验证码存储
 */
public interface CheckCodeStore {

    /**
     * @param key    key
     * @param value  value
     * @param expire 过期时间,单位秒
     */
    void set(String key, String value, Integer expire);

    String get(String key);

    void remove(String key);
}