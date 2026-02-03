package com.yy.homi.rbac.domain.convert;

import com.yy.homi.rbac.domain.dto.request.RoleInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysRole;
import com.yy.homi.rbac.domain.vo.RoleOptionVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SysRoleConvert {
    public static final SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);

    SysRole insertReqDTOToEntity(RoleInsertReqDTO roleInsertReqDTO);

    SysRole updateReqDTOToEntity(RoleUpdateReqDTO roleUpdateReqDTO);

    List<RoleOptionVO> toRoleOptionVOList(List<SysRole> sysRoleList);

}
