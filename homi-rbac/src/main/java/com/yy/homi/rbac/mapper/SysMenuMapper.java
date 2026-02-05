package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * 菜单权限表 数据层
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {


    List<SysMenu> selectMenuList(@Param("menuName") String menuName,
                                 @Param("menuType")String menuType,
                                 @Param("visible")Integer visible,
                                 @Param("status")Integer status);

    @Select("select * from sys_menu where menu_name = #{menuName}")
    SysMenu selectMenuByName(@Param("menuName") String menuName);

    SysMenu selectMenuByNameWithPid(@Param("menuName") String menuName, @Param("parentId")String parentId);

    void updateMenuById(SysMenu sysMenu);

    int hasChildByMenuId(@Param("menuId") String menuId);

    int updateStatusById(@Param("menuId") String menuId,@Param("status") int status);
    int updateVisibleById(@Param("menuId") String menuId,@Param("visible") int visible);

    List<SysMenu> selectVisibleMenusByUserId(@Param("userId") String userId);

    List<SysMenu> selectMenusByParentIdAndStatus(@Param("parentId") String parentId,@Param("status") int status);

    int updateStatusByIds(@Param("menuIds") List<String> menuIds,@Param("status") int status);

    List<SysMenu> selectList(@Param("menuName") String menuName, @Param("menuType") String menuType, @Param("visible") Integer visible, @Param("status") Integer status, @Param("beginTime") Date beginTime,@Param("endTime") Date endTime);
}