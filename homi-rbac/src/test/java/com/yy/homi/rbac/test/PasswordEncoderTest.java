package com.yy.homi.rbac.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
public class PasswordEncoderTest {

    @Test
    public void generatePassword() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // 生成加密后的字符串
        String encode = passwordEncoder.encode("123456");

        System.out.println("--------------------------------");
        System.out.println("加密后的密码为: " + encode);
        System.out.println("--------------------------------");

        // 校验一下是否匹配
        boolean matches = passwordEncoder.matches("123456", encode);
        System.out.println("比对结果: " + matches);
        log.error("{},{}",1,2);
    }
}