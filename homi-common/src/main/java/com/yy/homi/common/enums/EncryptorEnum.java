package com.yy.homi.common.enums;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

public enum EncryptorEnum {
    
    /**
     * BCrypt加密（Spring Security 默认，推荐）
     */
    BCRYPT {
        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        @Override
        public String encrypt(String password) {
            return encoder.encode(password);
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encoder.matches(rawPassword, encodedPassword);
        }
    },

    /**
     * MD5加密（传统方式，不安全，仅用于老系统兼容或简单校验）
     */
    MD5 {
        @Override
        public String encrypt(String password) {
            return DigestUtils.md5DigestAsHex(password.getBytes());
        }

        @Override
        public boolean matches(String rawPassword, String encodedPassword) {
            return encrypt(rawPassword).equalsIgnoreCase(encodedPassword);
        }
    };

    // 定义抽象方法，强制每个枚举项实现
    public abstract String encrypt(String password);
    public abstract boolean matches(String rawPassword, String encodedPassword);
}