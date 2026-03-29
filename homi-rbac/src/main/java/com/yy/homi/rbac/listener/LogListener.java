package com.yy.homi.rbac.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.common.event.SysLogEvent;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.mapper.SysLogMapper;
import com.yy.homi.rbac.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogListener {

    @Autowired
    private SysLogMapper sysLogMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 监听到日志事件后，执行入库操作
     *
     * @Async 表示异步执行，不会阻塞业务接口
     */
    @Async("homiExecutor") // 使用之前配置的线程池
    @EventListener
    public void saveSysLog(SysLogEvent event) {
        String operUserId = event.getSysLog().getOperUserId();

        if (StrUtil.isNotBlank(operUserId) && operUserId.equals(RbacConstants.SECURITY_DEFAULT_PRINCIPAL)) {
            //匿名用户，直接保存
            event.getSysLog().setOperUserId(null);
            sysLogMapper.insert(event.getSysLog());
        } else {
            //填充操作用户名
            Object sysUserJson = redisTemplate.opsForValue().get(RedisConstants.RBAC.USER_CACHE_PREFIX + operUserId);
            if (sysUserJson != null) {
                SysUserCache sysUserCache = JSON.parseObject(JSONObject.toJSONString(sysUserJson), SysUserCache.class);
                event.getSysLog().setOperName(sysUserCache.getNickName());
                sysLogMapper.insert(event.getSysLog());
            } else {
                SysUser sysUser = sysUserMapper.selectById(operUserId);
                if (sysUser != null) {
                    event.getSysLog().setOperName(sysUser.getNickName());
                    sysLogMapper.insert(event.getSysLog());
                }

                log.error("操作的operUserId对应用户不存在！");

            }

        }

    }
}