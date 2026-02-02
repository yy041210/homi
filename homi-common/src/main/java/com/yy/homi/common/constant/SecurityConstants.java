package com.yy.homi.common.constant;

public class SecurityConstants {

    /**
     * 认证请求头的 Key
     */
    public static final String AUTH_HEADER = "Authorization";

    /**
     * Token 前缀（通常用于 Header）
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     *  jwt 对称加密密钥
     */
    public static final String JWT_SINGING_SECRET= "homi_secret_key_1234567890_abcdefg";

    /**
     * 客户端密钥：类似于密码
     *置类里用 passwordEncoder.encode(CLIENT_SECRET)
     */
    public static final String CLIENT_SECRET = "secret";

    /**
     * 网关透传的用户ID Header
     */
    public static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 网关透传的用户角色 Header
     */
    public static final String USER_ROLE_HEADER = "X-User-Role";

    /**
     * 网关透传的用户名 Header (可选)
     */
    public static final String USER_NAME_HEADER = "X-User-Name";

    public static final class Clients{
        public static final String WEC_PC = "homi_web_pc"; //pc端的client id
        public static final String APP_IOS = "homi_app_ios";
        public static final String APP_ANDROID = "homi_app_android";
    }

    public static final class Scopes{
        public static final String ALL= "all";
        public static final String READ ="read";
        public static final String WRITE = "write";
        public static final String SERVER = "server";
    }


}
