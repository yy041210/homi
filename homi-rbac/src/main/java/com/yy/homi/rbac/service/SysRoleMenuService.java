package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysRoleMenu;

public interface SysRoleMenuService extends IService<SysRoleMenu> {
    R removeRoleMenuRelation(String roleId, String menuId);
}
