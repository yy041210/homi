package com.yy.homi.rbac.strategy.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;
import com.yy.homi.rbac.strategy.UserLoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class PhoneLoginStrategy implements UserLoginStrategy {

    @Autowired
    private AuthenticationManager authenticationManager; // Spring Security 认证管理器，负责校验账号密码
    @Autowired
    private ClientDetailsService clientDetailsService; // OAuth2 客户端信息服务，用于查询 client_id 相关配置
    @Resource(name = "tokenServices")
    private AuthorizationServerTokenServices authorizationServerTokenServices; //注入自己配置的令牌服务的实现类，用于生成、刷新、获取 Token。这里来自AuthorizationServerConfig
    @Override
    public String getLoginType() {
        return LoginReqDTO.LOGIN_TYPE_PHONE; //phone
    }

    @Override
    public R login(LoginReqDTO loginReqDTO) {
        return R.fail("服务未开通!");
    }
}
