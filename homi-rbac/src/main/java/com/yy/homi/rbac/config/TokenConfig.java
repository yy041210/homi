package com.yy.homi.rbac.config;

import com.yy.homi.common.constant.SecurityConstants;
import com.yy.homi.rbac.domain.entity.SysUserDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class TokenConfig {


    //令牌存储策略（jwt）
    @Bean
    public TokenStore tokenStore(){
        return new JwtTokenStore(jwtAccessTokenConverter());
    }

    /**
     * 令牌转换器
     * @return
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter(){
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setSigningKey(SecurityConstants.JWT_SINGING_SECRET); //设置jwt对称加密密钥
        return jwtAccessTokenConverter;
    }

    /**
     * 自定义令牌增强器：将SysUserDetails 里的额外信息塞入 JWT
     */
    @Bean
    public TokenEnhancer tokenEnhancer() {
        return (accessToken, authentication) -> {
            if (authentication.getPrincipal() instanceof SysUserDetails) {
                SysUserDetails sysUserDetails = (SysUserDetails) authentication.getPrincipal();
                Map<String, Object> info = new HashMap<>();
                // 扩展字段
                info.put("userId", sysUserDetails.getId());
                info.put("userName",sysUserDetails.getUsername());
                info.put("nickName", sysUserDetails.getNickName());
                ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(info);
            }
            return accessToken;
        };
    }

}
