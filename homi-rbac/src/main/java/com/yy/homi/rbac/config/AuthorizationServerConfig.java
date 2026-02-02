package com.yy.homi.rbac.config;

import com.yy.homi.common.constant.SecurityConstants;
import com.yy.homi.common.enums.ServiceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import java.util.Arrays;

@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig implements AuthorizationServerConfigurer {

    @Autowired
    private TokenStore tokenStore;
    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;
    @Autowired
    private TokenEnhancer tokenEnhancer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ClientDetailsService clientDetailsService;


    /**
     *  授权码存储服务,一般不适用授权码模式
     * 用于授权码模式 (authorization_code)，生成的 code 临时存放在内存中
     */
//    @Bean
//    public AuthorizationCodeServices authorizationCodeServices() {
//        return new InMemoryAuthorizationCodeServices();
//    }


    /**
     * 2. 令牌管理服务
     */
    @Bean
    public AuthorizationServerTokenServices tokenServices() {
        DefaultTokenServices service = new DefaultTokenServices();
        service.setClientDetailsService(clientDetailsService); // 绑定客户端服务
        service.setSupportRefreshToken(true); //是否刷新令牌
        service.setTokenStore(tokenStore);  //令牌存储策略

        // 增强链：先加字段，再做 JWT 签名
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();

        // 顺序很重要：先加字段(tokenEnhancer)，再做签名加密(jwtAccessTokenConverter)
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(tokenEnhancer, jwtAccessTokenConverter));
        service.setTokenEnhancer(tokenEnhancerChain);

        service.setAccessTokenValiditySeconds(7200); //令牌有效期 2h
        service.setRefreshTokenValiditySeconds(259200); //刷新令牌默认有效期3天
        return service;
    }


    /**
     * 配置：客户端详情信息 (谁能申请令牌)
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient(SecurityConstants.Clients.WEC_PC)        //客户端ID：区分 Web、App、小程序等不同终端"homi-web-pc","homi-app-ios","homi-app-android"                                // 客户端ID
                .secret(passwordEncoder.encode(SecurityConstants.CLIENT_SECRET))               // 客户端密钥 secret
                .resourceIds(ServiceType.GATEWAY.getServiceId())        // 资源服务ID：定义该令牌的访问边界,建议：如果通过网关统一鉴权，则设为网关服务ID；若需微服务级校验，则传入多个服务ID
//                .authorizedGrantTypes("authorization_code", "password", "client_credentials", "implicit", "refresh_token") // 支持的授权模式
                .authorizedGrantTypes("password", "client_credentials", "implicit", "refresh_token")
                .scopes(SecurityConstants.Scopes.ALL)                                          // 授权范围,all,read,write,server(仅限微服务内网互相调用)
                .autoApprove(false);                                   // 授权码模式不自动确认
//                .redirectUris("http://www.baidu.com");                  // 只有授权码授权回调地址
    }

    /**
     * 配置：令牌访问端点
     * 定义“获取令牌”这个接口（/oauth/token）的行为
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints
                .authenticationManager(authenticationManager)           // 密码模式必须
//                .authorizationCodeServices(authorizationCodeServices()) // 授权码存储服务
                .tokenServices(tokenServices())                         // 绑定上面配置的令牌管理服务
                .allowedTokenEndpointRequestMethods(HttpMethod.POST);   // 强制使用 POST 请求申请令牌
    }

    /**
     * 配置：令牌端点的安全约束
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security
                .tokenKeyAccess("permitAll()")              // 允许所有人访问 /oauth/token_key 获取密钥
                .checkTokenAccess("permitAll()")            // 允许所有人访问 /oauth/check_token 检查令牌
                .allowFormAuthenticationForClients();       // 允许客户端使用表单认证（即在 Body 传 id/secret）
    }

}
