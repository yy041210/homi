package com.yy.homi.thirdparty.interfaces;

/**
 * @description 验证码生成器
 */
public interface CheckCodeGenerator {
    /**
     * 验证码生成
     *
     */
    String generate(int length);


}