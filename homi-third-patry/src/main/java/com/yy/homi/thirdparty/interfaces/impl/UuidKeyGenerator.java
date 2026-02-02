package com.yy.homi.thirdparty.interfaces.impl;

import com.yy.homi.thirdparty.interfaces.KeyGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

//uuid生成器
@Component
public class UuidKeyGenerator implements KeyGenerator {
    @Override
    public String generate(String prefix) {
        String uuid = UUID.randomUUID().toString();
        return prefix + uuid.replaceAll("-", "");
    }
}
