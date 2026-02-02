package com.yy.homi.rbac.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.UserInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.UserUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.domain.dto.request.UserPageListResDTO;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    R pageList(UserPageListResDTO userPageListResDTO);

    R getUserInfo(String id);

    R insertUser(UserInsertReqDTO userInsertReqDTO);


    R updateUserById(UserUpdateReqDTO userUpdateReqDTO);

    R deleteUserById(String userId);

    R deleteUsers(List<String> userIds);

    R changeStatus(String userId);

    R addUserRoleRelation(String userId, String roleId);
}
