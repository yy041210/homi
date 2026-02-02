package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 统一登录请求参数
 */
@Data
@Schema(description = "统一登录请求参数")
public class LoginReqDTO implements Serializable {

    public static final String LOGIN_TYPE_PASSWORD = "password";
    public static final String LOGIN_TYPE_WECHAT = "wechat";
    public static final String LOGIN_TYPE_PHONE = "phone";


    /**
     * 登录类型：password(账号密码), mobile(手机验证码), wechat(微信扫码)
     * 对应 LoginContext 中的 strategyMap 的 Key
     */
    @Schema(description = "登录类型：password(账号密码), phone(手机验证码), wechat(微信扫码)", example = "password", required = true)
    @NotBlank(message = "登录类型不能为空")
    private String type;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "手机号", example = "13800000000")
    private String phone;

    @Schema(description = "验证码（短信或图片）", example = "8888")
    private String code;

    @Schema(description = "验证码唯一标识(Redis Key后缀)", example = "uuid-xxx-xxx")
    private String uuid;

    @Schema(description = "微信扫码登陆时，每一个二维码都绑定一个场景id", example = "uuid-xxx-xxx")
    private String sceneId;

    @Schema(description = "是否记住我", example = "true")
    private boolean rememberMe;

    // 扩展字段：可以存放一些设备信息、IP地址、客户端版本等 ..
}