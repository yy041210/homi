package com.yy.homi.common.constant;

public class RedisConstants {
    //rbac模块的key
    public static class RBAC{
        public static final String USER_CACHE_PREFIX = "homi:rbac:user:cache:";
        public static final long USER_CACHE_EXPIRE = 2 ; //用户缓存的TTL ，2h
    }

    //三方模块的key
    public static class THIRD{
        public static final String CHECK_CODE_PREFIX = "homi:thirdParty:checkCode:";
    }
}
