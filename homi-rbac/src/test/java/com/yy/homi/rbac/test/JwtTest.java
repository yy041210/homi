package com.yy.homi.rbac.test;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import com.yy.homi.common.constant.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;

@Slf4j
public class JwtTest {


    @Test
    public void testJwt() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiaG9taS1nYXRld2F5Il0sInVzZXJfbmFtZSI6Im9JWk9IMjczeGdOMUtSMWN0UTJaelR2dzFDTEkiLCJuaWNrTmFtZSI6IuaciemprOi0teWwhiIsInNjb3BlIjpbImFsbCJdLCJleHAiOjE3NzAwMDU3NjQsInVzZXJOYW1lIjoib0laT0gyNzN4Z04xS1IxY3RRMlp6VHZ3MUNMSSIsInVzZXJJZCI6IjIwMTcxNzEwOTMxMDIzNzA4MTciLCJqdGkiOiJwcWt5bDR1SzBIc2t1WUtCUU12QWN5Q1cxcnciLCJjbGllbnRfaWQiOiJob21pX3dlYl9wYyJ9.f1vfIAO21VNJIHpOnwNe3aJ3HcmiU3O4UMDlg73FNo8";

        // 1. 校验 Token 真实性
        boolean isValid = verifyJwt(token, SecurityConstants.JWT_SINGING_SECRET); // 注意：这里的密钥必须大于等于 32 位字符，否则校验会报错
        log.info("Token 签名验证是否通过: {}", isValid);

        // 2. 如果通过，则解析内容
        if (isValid) {
            Map<String, Object> claims = parseJwt(token);
            log.info("解析出的用户信息: {}", claims);
        }
    }

    /**
     * 方法一：校验 JWT 的签名和合法性
     * 作用：确定这个 Token 是不是我们自己发的，有没有被篡改过
     */
    public boolean verifyJwt(String token, String secret) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            // 使用密钥创建验证器
            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            // 返回验证结果
            return signedJWT.verify(verifier);
        } catch (Exception e) {
            log.error("JWT 校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 方法二：解析 JWT 里的 Payload 内容
     * 作用：把里面的 userId, nickName 等数据读出来
     */
    public Map<String, Object> parseJwt(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            // 拿到载荷中的所有 Claims
            return signedJWT.getJWTClaimsSet().getClaims();
        } catch (Exception e) {
            log.error("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}