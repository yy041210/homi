package com.yy.homi.rbac.domain.vo;

import com.yy.homi.rbac.domain.entity.SysRole;
import lombok.Data;

import java.util.List;

@Data
public class RoleInfoVO {
    // 角色基本信息
    private SysRole role;

    // 该角色拥有的菜单ID集合（用于前端树组件的回显）
    private List<String> menuIds;
}