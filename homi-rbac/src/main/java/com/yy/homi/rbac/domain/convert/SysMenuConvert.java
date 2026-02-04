package com.yy.homi.rbac.domain.convert;

import com.yy.homi.rbac.domain.dto.request.MenuSaveReqDTO;
import com.yy.homi.rbac.domain.entity.SysMenu;
import com.yy.homi.rbac.domain.vo.MenuOptionVO;
import com.yy.homi.rbac.domain.vo.MenuTreeVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper(componentModel = "spring")
public interface SysMenuConvert {
    public static final SysMenuConvert INSTANCE = Mappers.getMapper(SysMenuConvert.class);

    SysMenu menuSaveReqToEntity(MenuSaveReqDTO menuInsertReqDTO);
    MenuTreeVO toMenuTreeVO(SysMenu sysMenu);
    List<MenuTreeVO> toMenuTreeVOList(List<SysMenu> sysMenuList);

    List<MenuOptionVO> toMenuOptionVOList(List<SysMenu> sysMenuList);
}
