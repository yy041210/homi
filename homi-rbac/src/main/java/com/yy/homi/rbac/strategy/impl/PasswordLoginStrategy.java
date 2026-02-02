package com.yy.homi.rbac.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.constant.SecurityConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.rbac.domain.convert.SysUserConvert;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;
import com.yy.homi.rbac.domain.entity.SysUserDetails;
import com.yy.homi.rbac.mapper.SysUserMapper;
import com.yy.homi.rbac.strategy.UserLoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PasswordLoginStrategy implements UserLoginStrategy{
    @Autowired
    private AuthenticationManager authenticationManager; // Spring Security 认证管理器，负责校验账号密码
    @Autowired
    private ClientDetailsService clientDetailsService; // OAuth2 客户端信息服务，用于查询 client_id 相关配置
    @Resource(name = "tokenServices")
    private AuthorizationServerTokenServices authorizationServerTokenServices; //注入自己配置的令牌服务的实现类，用于生成、刷新、获取 Token。这里来自AuthorizationServerConfig

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SysUserConvert sysUserConvert;

    @Autowired
    private SysUserMapper sysUserMapper;


    @Override
    public String getLoginType() {
        return LoginReqDTO.LOGIN_TYPE_PASSWORD; //password
    }

    @Override
    public R login(LoginReqDTO loginReqDTO) {
        //1.参数校验
        String username = loginReqDTO.getUsername();
        String password = loginReqDTO.getPassword();
        if(StrUtil.isBlank(username) && StrUtil.isBlank(password)){
            return R.fail("账号密码不能为空");
        }
        try {
            // 2. 构造认证令牌对象（未认证状态）
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, password);

            // 3. 执行认证：管理器会调用 UserDetailsService 查数据库，并用 PasswordEncoder 比对密码
            // 如果认证失败（密码错、用户不存在），会在此处直接抛出异常
            Authentication authentication = authenticationManager.authenticate(authenticationToken);

            // 4. 认证成功，从结果中提取用户信息（由 UserDetailsService 返回的实现类）
            SysUserDetails userDetails = (SysUserDetails) authentication.getPrincipal();

            // 5. 获取 OAuth2 客户端详情（检查是否存在 homi-client 这个客户端，读取其配置的有效期、范围等）
            String clientId = SecurityConstants.Clients.WEC_PC;  //homi_web_pc
            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

            // 6. 构建 OAuth2 令牌请求上下文
            // 参数：请求参数Map、客户端ID、权限范围、授权模式(password)
            TokenRequest tokenRequest = new TokenRequest(new HashMap<>(), clientId, clientDetails.getScope(), "password");

            // 7. 合并生成 OAuth2Request
            OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);

            // 8. 结合“用户信息”与“客户端信息”，生成最终的 OAuth2 认证对象
            OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

            // 9. 调用令牌服务：生成真正的 JWT 令牌（内部会触发你配置的 TokenEnhancer 增强器）
            OAuth2AccessToken accessToken = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);

            // 10. 将用户信息同步到 Redis 缓存，方便后续微服务之间共享
//            cacheUserInfo(userDetails);
            SysUserCache sysUserCache = sysUserConvert.userDetailsToUserCache(userDetails);
            sysUserCache.setUserName(userDetails.getUsername());
            String userId = sysUserCache.getId();
            List<String> permissonList = sysUserMapper.selectUserPermissionsById(userId);
            Set<String> permissions = new HashSet<>(permissonList);  //查询用户的 角色和权限标识["ROLE_admin","system:user:add",...]
            sysUserCache.setPermissions(permissions);
            String userCacheKey = RedisConstants.RBAC.USER_CACHE_PREFIX + userId;
            redisTemplate.opsForValue().set(userCacheKey,sysUserCache,RedisConstants.RBAC.USER_CACHE_EXPIRE, TimeUnit.HOURS);  //homi:rbac:cache:user:${userId}

            // 12. 封装响应数据
            Map<String, Object> result = new HashMap<>();
            result.put("access_token", accessToken.getValue()); // JWT 字符串
            result.put("expires_in", accessToken.getExpiresIn()); // 有效期 秒
            result.put("user_id", userDetails.getId()); // 用户id
            return R.ok(result);
        } catch (BadCredentialsException e) {
            throw new ServiceException("用户登录失败，密码错误:"+username);
        } catch (Exception e) {
            throw new ServiceException("登录系统异常!异常信息:"+e.getMessage());
        }
    }
}
