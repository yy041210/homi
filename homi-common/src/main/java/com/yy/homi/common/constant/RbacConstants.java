package com.yy.homi.common.constant;

public class RbacConstants {
    /**
     * 角色标识
     */
    public static final String ADMIN_ROLE_ID = "1";
    public static final String ADMIN_ROLE_KEY = "ROLE_admin";

    /** * 菜单类型
     */
    public static final String TYPE_DIR = "M";   // 目录
    public static final String TYPE_MENU = "C";  // 菜单
    public static final String TYPE_AUTH = "F";  // 权限/按钮

    /** * 显隐状态 (对应 visible 字段)
     */
    public static final int MENU_VISIBLE = 0;    // 显示
    public static final int MENU_HIDDEN = 1;     // 隐藏

    /**
     * 用户性别常量
     */
    public static final int SEX_MALE = 0; //男
    public static final int SEX_FEMALE = 1; //女
    public static final int SEX_UNKNOWN = 2; //未知

    /**
     * 微信扫码登录，第一次默认创建账号
     * 用户名：homi + 毫秒级时间戳(36进制) + 2位随机数
     * 密码：123456
     */
    public static final String DEFAULT_USERNAME_PREFIX = "homi_";
    public static final String DEFAULT_PASSWORD = "123456";

    /** * 顶级节点标识
     */
    public static final String TOP_NODE_ID = "0";

}
