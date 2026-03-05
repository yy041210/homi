package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.entity.SysRoleMenu;
import com.yy.homi.rbac.mapper.SysRoleMenuMapper;
import com.yy.homi.rbac.service.SysRoleMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, SysRoleMenu> implements SysRoleMenuService {

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public R removeRoleMenuRelation(String roleId, String menuId) {
        if(StrUtil.isEmpty(roleId) || StrUtil.isEmpty(menuId)){
            return R.fail("角色id和菜单id不能为空！");
        }
        sysRoleMenuMapper.deleteByRoleIdAndMenuId(roleId,menuId);
        return R.ok();
    }
}
