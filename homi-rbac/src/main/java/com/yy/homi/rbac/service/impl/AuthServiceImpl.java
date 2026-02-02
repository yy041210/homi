package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public R logout(String userId) {
        if(StrUtil.isBlank(userId)){
            return R.fail("用户id不能为空！");
        }
        // 2. 清理 Redis 中的登录状态或 Token 信息
        // 假设存储 key 格式为 "login:token:123"
        String key = RedisConstants.RBAC.USER_CACHE_PREFIX + userId;
        Boolean deleted = redisTemplate.delete(key);

        if (BooleanUtil.isTrue(deleted)) {
            // 3. (可选) 如果有 SecurityContext，也可以手动清理
            SecurityContextHolder.clearContext();
            return R.ok("登出成功!");
        }

        return R.fail("登出失败，用户可能已离线");
    }
}
