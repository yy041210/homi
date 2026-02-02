package com.yy.homi.thirdparty.interfaces.impl;

import com.yy.homi.thirdparty.interfaces.CheckCodeStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description 使用本地内存存储验证码，测试用
 */
@Component
public class MemoryCheckCodeStore implements CheckCodeStore {

    public static final Map<String,String> CHECK_CODE_STORE = new ConcurrentHashMap<>();

    @Override
    public void set(String key, String value, Integer expire) {
        CHECK_CODE_STORE.put(key,value);
    }

    @Override
    public String get(String key) {
        return CHECK_CODE_STORE.get(key);
    }

    @Override
    public void remove(String key) {
        CHECK_CODE_STORE.remove(key);
    }
}
