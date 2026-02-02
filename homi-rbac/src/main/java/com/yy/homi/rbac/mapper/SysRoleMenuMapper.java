package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
    int checkMenuExistRole(@Param("menuId") String menuId);

    List<String> selectMenuIdsByRoleId(@Param("roleId") String roleId);

    int insertBatch(@Param("roleId") String roleId, @Param("menuIds") List<String> menuIds);

    int deleteByRoleId(@Param("roleId") String roleId);

    @Select("select count(1) from sys_role_menu where role_id = #{roleId} and menu_id = #{menuId}")
    int countByRoleIdAndMenuId(@Param("roleId") String roleId, @Param("menuId") String menuId);

    @Insert("insert into sys_role_menu(role_id,menu_id) values(#{roleId},#{menuId})")
    int addRoleMenu(@Param("roleId") String roleId, @Param("menuId") String menuId);
}