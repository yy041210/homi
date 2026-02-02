package com.yy.homi.rbac.domain.convert;

import com.yy.homi.rbac.domain.dto.request.RoleInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysRole;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface SysRoleConvert {
    public static final SysRoleConvert INSTANCE = Mappers.getMapper(SysRoleConvert.class);

    SysRole insertReqDTOToEntity(RoleInsertReqDTO roleInsertReqDTO);

    SysRole updateReqDTOToEntity(RoleUpdateReqDTO roleUpdateReqDTO);

}
