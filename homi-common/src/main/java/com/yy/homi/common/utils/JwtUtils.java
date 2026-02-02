package com.yy.homi.common.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtUtils {

    // 密钥：必须至少 256 位（32个字符），且要与你 homi-rbac 中的 SIGNING_KEY 一致
    private static final String SECRET = "homi_secret_key";
    // 过期时间：2小时
    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000L;

    /**
     * 生成 JWT 令牌
     * @param payload 自定义载荷（如 user_name, authorities）
     */
    public static String createToken(Map<String, Object> payload, String subject) {
        try {
            // 1. 创建 HMAC 签名器
            JWSSigner signer = new MACSigner(SECRET);

            // 2. 建立 JWT 载荷 (Claims)
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject(subject) // 主题（通常是用户名）
                    .issuer("homi-auth") // 签发者
                    .issueTime(new Date()) // 签发时间
                    .expirationTime(new Date(System.currentTimeMillis() + EXPIRE_TIME)); // 过期时间

            // 注入自定义属性
            payload.forEach(builder::claim);
            JWTClaimsSet claimsSet = builder.build();

            // 3. 建立签名对象
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

            // 4. 签名并序列化
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("签名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析并校验 JWT 令牌
     * @return 返回 ClaimsSet，如果失效或伪造则返回 null
     */
    public static JWTClaimsSet parseAndVerify(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            // 1. 创建校验器
            JWSVerifier verifier = new MACVerifier(SECRET);
            
            // 2. 验证签名
            if (!signedJWT.verify(verifier)) {
                log.error("JWT 签名验证失败！");
                return null;
            }

            // 3. 验证是否过期
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (new Date().after(claimsSet.getExpirationTime())) {
                log.error("JWT 已过期！");
                return null;
            }

            return claimsSet;
        } catch (Exception e) {
            log.error("JWT 解析异常: {}", e.getMessage());
            return null;
        }
    }
}