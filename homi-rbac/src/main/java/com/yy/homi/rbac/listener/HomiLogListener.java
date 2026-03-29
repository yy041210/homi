package com.yy.homi.rbac.listener;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yy.homi.common.constant.RabbitMqConstants;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.entity.SysLog;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.mapper.SysLogMapper;
import com.yy.homi.rbac.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HomiLogListener {

    @Autowired
    private SysLogMapper sysLogMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private RedisTemplate redisTemplate;


    @RabbitListener(queues = {RabbitMqConstants.HOMI_LOG_QUEUE})
    public void saveSysLog(SysLog sysLog) {
        String operUserId = sysLog.getOperUserId();

        if (StrUtil.isNotBlank(operUserId) && operUserId.equals(RbacConstants.SECURITY_DEFAULT_PRINCIPAL)) {
            //匿名用户，直接保存
            sysLog.setOperUserId(null);
            sysLogMapper.insert(sysLog);
        } else {
            //填充操作用户名
            Object sysUserJson = redisTemplate.opsForValue().get(RedisConstants.RBAC.USER_CACHE_PREFIX + operUserId);
            if (sysUserJson != null) {
                SysUserCache sysUserCache = JSON.parseObject(JSONObject.toJSONString(sysUserJson), SysUserCache.class);
                sysLog.setOperName(sysUserCache.getNickName());
                sysLogMapper.insert(sysLog);
            } else {
                SysUser sysUser = sysUserMapper.selectById(operUserId);
                if (sysUser != null) {
                    sysLog.setOperName(sysUser.getNickName());
                    sysLogMapper.insert(sysLog);
                }

                log.error("操作的operUserId对应用户不存在！");

            }

        }

    }
}