package com.yy.homi.thirdparty.interfaces;

//
public interface KeyGenerator {

    /**
     * key生成
     *
     * @return 验证码
     */
    String generate(String prefix);
}