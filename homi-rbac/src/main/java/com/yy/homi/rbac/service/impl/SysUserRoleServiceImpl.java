package com.yy.homi.rbac.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.rbac.domain.entity.SysUserRole;
import com.yy.homi.rbac.mapper.SysUserRoleMapper;
import com.yy.homi.rbac.service.SysUserRoleService;
import org.springframework.stereotype.Service;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {
}
