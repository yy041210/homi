package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Select("select count(1) from sys_user_role where role_id = #{roleId}")
    int countUserByRoleId(@Param("roleId") String roleId);

    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(@Param("userId") String userId);

    int deleteByUserIds(@Param("userIds") List<String> userIds);

    int countByRelation(@Param("userId") String userId, @Param("roleId") String roleId);
}