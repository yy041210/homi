package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 角色表 数据层
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {


    List<SysRole> selectRoleList(@Param("roleName") String roleName,
                                 @Param("roleKey") String roleKey,
                                 @Param("status") Integer status,
                                 @Param("beginTime") Date beginTime,
                                 @Param("endTime") Date endTime);



    @Select("select * from sys_role where ( role_name = #{roleName} or role_key = #{roleKey} ) and id != #{roleId} limit 1")
    SysRole selectOneByNameOrKey(@Param("roleName") String roleName, @Param("roleKey") String roleKey,@Param("roleId") String roleId);

    @Delete("delete from sys_role_menu where role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") String roleId);
}