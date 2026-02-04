package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.MenuSaveReqDTO;
import com.yy.homi.rbac.domain.dto.request.MenuPageListReqDTO;
import com.yy.homi.rbac.domain.entity.SysMenu;

public interface SysMenuService extends IService<SysMenu> {
    R pageList(MenuPageListReqDTO menuPageListReqDTO);

    R getMenuInfo(String id);

    R insertMenu(MenuSaveReqDTO menuSaveReqDTO);

    R updateMenu(MenuSaveReqDTO menuSaveReqDTO);

    R deleteMenuById(String id);

    R changeStatus(String id);

    R getMenuTree();

    R getMenuTreeByUserId(String userId);

    R changeVisible(String id);

    R listAll();
}
