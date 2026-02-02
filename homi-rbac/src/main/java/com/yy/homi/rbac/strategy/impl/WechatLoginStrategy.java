package com.yy.homi.rbac.strategy.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.constant.SecurityConstants;
import com.yy.homi.common.constant.ThirdPartyConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.common.enums.EncryptorEnum;
import com.yy.homi.common.exception.ServiceException;
import com.yy.homi.rbac.domain.convert.SysUserConvert;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.domain.entity.SysUserDetails;
import com.yy.homi.rbac.mapper.SysUserMapper;
import com.yy.homi.rbac.strategy.UserLoginStrategy;
import com.yy.homi.rbac.websocket.LoginWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WechatLoginStrategy implements UserLoginStrategy {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserConvert sysUserConvert;
    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private ClientDetailsService clientDetailsService; // OAuth2 客户端信息服务，用于查询 client_id 相关配置
    @Resource(name = "tokenServices")
    private AuthorizationServerTokenServices authorizationServerTokenServices; //注入自己配置的令牌服务的实现类，用于生成、刷新、获取 Token。这里来自AuthorizationServerConfig
    @Autowired
    private RestTemplate restTemplate;

    @Value("${wx.open.app-id}")
    private String appId;
    @Value("${wx.open.app-secret}")
    private String appSecret;

    @Override
    public String getLoginType() {
        return LoginReqDTO.LOGIN_TYPE_WECHAT; //wechat
    }

    @Override
    public R login(LoginReqDTO req) {
        String code = req.getCode();
        String sceneId = req.getSceneId();
        if (StrUtil.isBlank(code) || StrUtil.isBlank(sceneId)) {
            throw new ServiceException("微信回调成功，但是code或sceneId为空！");
        }

        //1.根据code请求微信获取access_token
        String getUrl = String.format(ThirdPartyConstants.WECHAT_CODE_TO_TOKEN_URL, appId, appSecret, code);
        String responseStr = restTemplate.getForObject(getUrl, String.class);
        JSONObject jsonObject = JSON.parseObject(responseStr);
        if (jsonObject == null) {
            throw new ServiceException("请求access_token 返回值为null!");
        }
        String token = jsonObject.getString("access_token");
        String openId = jsonObject.getString("openid");
        if (StrUtil.isBlank(token) || StrUtil.isBlank(openId)) {
            throw new ServiceException("微信回调成功，请求token或openid为空");
        }

        //openid == userName在我们系统中
        SysUser sysUser = sysUserMapper.selectByUserNameNeId(openId, "");
        if(sysUser == null){
            //用户不存在就创建一个,密码默认为123456
            //根据openid和token获取用户信息
            String getUserInfoUrl = String.format(ThirdPartyConstants.WECHAT_GET_USERINFO_URL, token, openId); //https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN
            String userInfoJsonStr = restTemplate.getForObject(getUserInfoUrl, String.class);
            JSONObject userInfoJson = JSON.parseObject(userInfoJsonStr);
            if (userInfoJson == null) {
                throw new ServiceException("请求用户详情 返回值为null!");
            }

            //解析数据
            String nickName = userInfoJson.getString("nickname");
            Integer sex = userInfoJson.getInteger("sex") == null  ? 2 : userInfoJson.getInteger("sex");
            String avatar = userInfoJson.getString("headimgurl") == null ? "" : userInfoJson.getString("headimgurl");
            SysUser saveSysUser = new SysUser();
            saveSysUser.setUserName(openId);
            saveSysUser.setNickName(nickName);
            saveSysUser.setSex(sex);
            saveSysUser.setAvatar(avatar);
            saveSysUser.setPassword(EncryptorEnum.BCRYPT.encrypt("123456"));
            int rows = sysUserMapper.insert(saveSysUser);
            if(rows <= 0){
                throw new ServiceException("微信第一次登录 插入用户信息失败！");
            }
        }


        // 2. 跳过密码校验
        UserDetails userDetails = userDetailsService.loadUserByUsername(openId);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());  //这里填入第三个参数时，authentication默认就是已认证,就不用在执行3的密码验证了
        // 3. 执行认证：管理器会调用 UserDetailsService 查数据库，并用 PasswordEncoder 比对密码
        // 如果认证失败（密码错、用户不存在），会在此处直接抛出异常
//        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 4. 获取 OAuth2 客户端详情（检查是否存在 homi-client 这个客户端，读取其配置的有效期、范围等）
        String clientId = SecurityConstants.Clients.WEC_PC;  //homi_web_pc
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        // 5. 构建 OAuth2 令牌请求上下文
        // 参数：请求参数Map、客户端ID、权限范围、授权模式(password)
        TokenRequest tokenRequest = new TokenRequest(new HashMap<>(), clientId, clientDetails.getScope(), "password");

        // 6. 合并生成 OAuth2Request
        OAuth2Request oAuth2Request = tokenRequest.createOAuth2Request(clientDetails);

        // 7. 结合“用户信息”与“客户端信息”，生成最终的 OAuth2 认证对象
        OAuth2Authentication oAuth2Authentication = new OAuth2Authentication(oAuth2Request, authentication);

        // 8. 调用令牌服务：生成真正的 JWT 令牌（内部会触发你配置的 TokenEnhancer 增强器）
        OAuth2AccessToken accessToken = authorizationServerTokenServices.createAccessToken(oAuth2Authentication);

        //9.存入redis缓存中
        SysUserCache sysUserCache = sysUserConvert.userDetailsToUserCache((SysUserDetails) userDetails);
        sysUserCache.setUserName(userDetails.getUsername());
        String userId = sysUserCache.getId();
        List<String> permissonList = sysUserMapper.selectUserPermissionsById(userId);
        Set<String> permissions = new HashSet<>(permissonList);  //查询用户的 角色和权限标识["ROLE_admin","system:user:add",...]
        sysUserCache.setPermissions(permissions);
        String userCacheKey = RedisConstants.RBAC.USER_CACHE_PREFIX + userId;
        redisTemplate.opsForValue().set(userCacheKey,sysUserCache,RedisConstants.RBAC.USER_CACHE_EXPIRE, TimeUnit.HOURS);  //homi:rbac:cache:user:${userId}

        // 10. 封装响应数据,
        Map<String, Object> result = new HashMap<>();
        result.put("access_token", accessToken.getValue()); // JWT 字符串
        result.put("expires_in", accessToken.getExpiresIn()); // 有效期 秒
        result.put("user_id", userId); // 用户id
        LoginWebSocketServer.sendMessage(sceneId,R.ok(result));  //写出webSocket
        return R.ok(result);
    }
}
