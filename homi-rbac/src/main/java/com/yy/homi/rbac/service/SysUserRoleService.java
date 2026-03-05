package com.yy.homi.rbac.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysUserRole;

public interface SysUserRoleService extends IService<SysUserRole> {
    R removeUserRoleRelation(String userId, String roleId);
}
