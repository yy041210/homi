package com.yy.homi.rbac.service.impl;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;
import com.yy.homi.rbac.feign.CheckCodeFeign;
import com.yy.homi.rbac.strategy.UserLoginStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class LoginStrategyContext {
    public static final Map<String, UserLoginStrategy> USER_LOGIN_STRATEGY_MAP = new ConcurrentHashMap();


    @Autowired
    private CheckCodeFeign checkCodeFeign;

    @Autowired  //构造方法注入所有 实现UserLoginStrategy的Bean
    public LoginStrategyContext(List<UserLoginStrategy> userLoginStrategyList){
        //存入到map中，方便通过类型拿去使用对应的策略实例
        userLoginStrategyList
                .stream()
                .forEach(strategy -> {
                    USER_LOGIN_STRATEGY_MAP.put(strategy.getLoginType(),strategy);
                    log.info("加载 {} 模式登录策略实例完成",strategy.getLoginType());
                });
    }

    public R login(LoginReqDTO req){
        String type = req.getType();
        UserLoginStrategy userLoginStrategy = USER_LOGIN_STRATEGY_MAP.get(type);
        if(userLoginStrategy == null){
            return R.fail("没有该类型的登录策略!");
        }
        //校验验证码
        if(!type.equals(LoginReqDTO.LOGIN_TYPE_WECHAT)){
            //密码或手机登录需要验证码验证
            R r = checkCodeFeign.verifyCheckCode(req.getUuid(), req.getCode());
            if(r.getCode() != HttpStatus.OK.value()){
                return r;
            }
        }
        return userLoginStrategy.login(req);
    }

}
