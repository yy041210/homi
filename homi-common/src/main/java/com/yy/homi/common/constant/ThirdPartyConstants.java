package com.yy.homi.common.constant;

public class ThirdPartyConstants {

    /**
     *微信扫码登录 snsapi_type
     */
    public static final String WECHAT_LOGIN_SNS_BASE = "snsapi_base";  //静默收取按，只能获取openid
    public static final String WECHAT_LOGIN_SNS_USERINFO = "snsapi_userinfo";  //弹出授权页。可以获取头像、昵称、性别、城市等详细信息


    /**
     * 微信官方二维码登录地址
     */
    public static final  String WECHAT_QRCODE_LOGIN_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s&forcePopup=true#wechat_redirect"; // 公众号授权地址模板,&forcePopup=true强制开启授权界面

    /**
     * 换取微信用户授权 Token
     */
    public static final String WECHAT_CODE_TO_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";

    /**
     * 根据opneid和token用户信息的token
     */
    public static final String WECHAT_GET_USERINFO_URL= "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN";

}
