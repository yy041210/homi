package com.yy.homi.rbac.strategy;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;

//登录策略接口,有微信登录，密码登录，电话号码登录多个实现
public interface UserLoginStrategy {
    /**
     * 判断当前策略是否支持该登录类型
     */
    String getLoginType();

    /**
     * 执行具体的登录逻辑
     */
    R login(LoginReqDTO loginReqDTO);
}
