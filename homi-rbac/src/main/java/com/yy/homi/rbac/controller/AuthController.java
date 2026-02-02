package com.yy.homi.rbac.controller;

import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.LoginReqDTO;
import com.yy.homi.rbac.service.AuthService;
import com.yy.homi.rbac.service.impl.LoginStrategyContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Tag(name = "01.认证管理", description = "用户登录、登出及权限校验")
@Validated
@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginStrategyContext loginStrategyContext;

    //用户登录接口
    @Operation(summary = "用户统一登录", description = "支持账号密码、手机号多种登录方式")
    @ResponseBody
    @PostMapping("/login")
    public R login(@Validated @RequestBody LoginReqDTO loginReqDTO){
        return loginStrategyContext.login(loginReqDTO);
    }

    //用户登出接口
    @Operation(summary = "用户登出接口",description = "用户退出登录")
    @ResponseBody
    @GetMapping("/logout")
    public R logout(@RequestParam("userId") @NotBlank String userId){
        return authService.logout(userId);
    }

    //用户微信扫码的登录的回调
    //因为使用模板引擎，请求体不能返回json
    @Operation(summary = "微信扫码的登录的回调")
    @GetMapping("/weChatLogin")
    public String weChatLogin(@RequestParam("code") @NotBlank String code,@RequestParam("state") @NotBlank String state){
        String sceneId = state;
        LoginReqDTO loginReqDTO = new LoginReqDTO();
        loginReqDTO.setType(LoginReqDTO.LOGIN_TYPE_WECHAT);
        loginReqDTO.setCode(code);
        loginReqDTO.setSceneId(sceneId);
        R result = loginStrategyContext.login(loginReqDTO);
        if(result.getCode() != HttpStatus.OK.value()){
            return "login_fail";
        }
        return "login_success";
    }
}
