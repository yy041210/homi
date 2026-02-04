package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.AddRoleMenusReqDTO;
import com.yy.homi.rbac.domain.dto.request.RolePageListReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysRole;

public interface SysRoleService extends IService<SysRole> {
    R pageList(RolePageListReqDTO rolePageListReqDTO);

    R getRoleInfo(String roleId);

    R insertRole(RoleInsertReqDTO roleInsertReqDTO);

    R updateRole(RoleUpdateReqDTO roleUpdateReqDTO);

    R deleteByRoleId(String roleId);

    R changeStatus(String roleId);

    R addRoleMenuRelation(AddRoleMenusReqDTO addRoleMenusReqDTO);

    R listAll();
}
