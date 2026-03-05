package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysUserRole;
import com.yy.homi.rbac.mapper.SysUserRoleMapper;
import com.yy.homi.rbac.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public R removeUserRoleRelation(String userId, String roleId) {
        if (StrUtil.isEmpty(userId) || StrUtil.isEmpty(roleId)) {
            return R.fail("userId和roleId不能为空！");
        }
        sysUserRoleMapper.deleteByUserIdAndRoleId(userId, roleId);
        return R.ok();
    }
}
