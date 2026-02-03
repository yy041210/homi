package com.yy.homi.rbac.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.convert.SysMenuConvert;
import com.yy.homi.rbac.domain.dto.request.MenuSaveReqDTO;
import com.yy.homi.rbac.domain.dto.request.MenuPageListReqDTO;
import com.yy.homi.rbac.domain.entity.SysMenu;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.domain.vo.MenuTreeVO;
import com.yy.homi.rbac.mapper.SysMenuMapper;
import com.yy.homi.rbac.mapper.SysRoleMenuMapper;
import com.yy.homi.rbac.mapper.SysUserMapper;
import com.yy.homi.rbac.service.SysMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    @Autowired
    private SysMenuMapper sysMenuMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private SysMenuConvert sysMenuConvert;


    @Override
    public R pageList(MenuPageListReqDTO req) {
        String menuName = req.getMenuName();
        String menuType = req.getMenuType();
        Integer visible = req.getVisible();
        Integer status = req.getStatus();

        //1.开启分页
        PageHelper.startPage(req.getPageNum(), req.getPageSize());

        //2。执行查询语句
        List<SysMenu> sysMenuList = sysMenuMapper.selectMenuList(menuName, menuType, visible, status);

        //3.封装对象
        PageInfo<SysMenu> pageInfo = new PageInfo<>(sysMenuList);
        return R.ok(pageInfo);
    }

    @Override
    public R getMenuInfo(String id) {
        SysMenu sysMenu = sysMenuMapper.selectById(id);
        if (sysMenu != null) {
            return R.ok(sysMenu);
        }
        return R.fail("菜单不存在！");
    }

    @Override
    @Transactional
    public R insertMenu(MenuSaveReqDTO req) {
        //1.处理请求参数
        R r = preProcessMenuSaveReqDTO(req);
        if (r != null) {
            return r;
        }

        if (req.getStatus() == null) {
            req.setStatus(CommonConstants.STATUS_ENABLED);  //正常正常
        }
        if (req.getVisible() == null) {
            req.setVisible(RbacConstants.MENU_VISIBLE); //默认显示
        }
        if (req.getOrderNum() == null) {
            req.setOrderNum(0);
        }
        //2.快速转换实体类
        SysMenu sysMenu = sysMenuConvert.menuSaveReqToEntity(req);

        //3.MenuName和Pid唯一性检查
        SysMenu menu = sysMenuMapper.selectMenuByNameWithPid(sysMenu.getMenuName(), sysMenu.getParentId());
        if (menu != null) {
            return R.fail("操作失败，当前层级下已存在该名称的节点");
        }
        sysMenuMapper.insert(sysMenu);
        return R.ok("新增成功");
    }

    @Override
    @Transactional
    public R updateMenu(MenuSaveReqDTO req) {
        if (StrUtil.isBlank(req.getId())) {
            return R.fail("菜单id不能为空");
        }

        R r = preProcessMenuSaveReqDTO(req);
        if (r != null) {
            return r;
        }
        //2.快速转换实体类
        SysMenu sysMenu = sysMenuConvert.menuSaveReqToEntity(req);
        sysMenuMapper.updateMenuById(sysMenu);
        return R.ok("修改成功");
    }

    @Override
    public R deleteMenuById(String id) {
        if (StrUtil.isBlank(id)) {
            return R.fail("id不能为空");
        }
        //1.查询是否存在下级菜单
        int childCount = sysMenuMapper.hasChildByMenuId(id);
        if (childCount > 0) {
            return R.fail("存在下级菜单,不允许删除");
        }
        //2.查询该菜单是否已被分配给角色
        if (sysRoleMenuMapper.checkMenuExistRole(id) > 0) {
            return R.fail("菜单已分配给角色,不允许删除");
        }

        //3.执行删除
        sysMenuMapper.deleteById(id);
        return R.ok("删除成功");
    }

    @Override
    public R changeStatus(String id) {

        // 1. 先查询当前数据
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            return R.fail("菜单不存在");
        }

        // 2. 状态取反 (假设 0正常 1停用)
        Integer newStatus = CommonConstants.STATUS_ENABLED == menu.getStatus()
                ? CommonConstants.STATUS_DISABLED
                : CommonConstants.STATUS_ENABLED;

        // 3. 如果是停用操作，需要递归停用所有下级（可选，根据业务需求）
        sysMenuMapper.updateStatusById(id, newStatus);

        return R.ok("更改状态成功");

    }

    @Override
    public R getMenuTree() {
        //1.查询所有次啊但按照orderNum升序状态正常
        List<SysMenu> sysMenuList = sysMenuMapper.
                selectAllMenus();
        if (CollectionUtil.isEmpty(sysMenuList)) {
            return R.ok(new ArrayList<>());
        }

        //2.将entity转为vo集合
        List<MenuTreeVO> menuTreeVOList = sysMenuConvert.toMenuTreeVOList(sysMenuList);

        Map<String, List<MenuTreeVO>> nodeMap = menuTreeVOList.stream().collect(Collectors.groupingBy(MenuTreeVO::getParentId));
        //3.递归
        List<MenuTreeVO> menuTreeVOs = buildMenuTreeRecursive("0", nodeMap);

        return R.ok(menuTreeVOs);
    }

    @Override
    public R getMenuTreeByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空！");
        }
        //判断用户是否存在
        SysUser sysUserDB = sysUserMapper.selectById(userId);
        if (sysUserDB == null) {
            return R.fail("用户不存在！");
        } else if (sysUserDB.getStatus() == CommonConstants.STATUS_DISABLED) {
            return R.fail("用户已被禁用！");
        }
        // 1. 查询该用户关联的所有非隐藏菜单（注意：SQL中要根据用户->角色->菜单）
        List<SysMenu> menus = sysMenuMapper.selectVisibleMenusByUserId(userId);
        List<MenuTreeVO> menuTreeVOList = sysMenuConvert.toMenuTreeVOList(menus);
        Map<String, List<MenuTreeVO>> nodeMap = menuTreeVOList.stream().collect(Collectors.groupingBy(MenuTreeVO::getParentId));
        List<MenuTreeVO> menuTreeVOS = buildMenuTreeRecursive(RbacConstants.TOP_NODE_ID, nodeMap);

        return R.ok(menuTreeVOS);
    }

    @Override
    public R changeVisible(String id) {
        // 1. 先查询当前数据
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            return R.fail("菜单不存在");
        }

        // 2. 状态取反 (假设 0正常 1停用)
        Integer newVisibleStatus = menu.getVisible() == RbacConstants.MENU_VISIBLE ? RbacConstants.MENU_HIDDEN : RbacConstants.MENU_VISIBLE;

        // 3. 如果是停用操作，需要递归停用所有下级（可选，根据业务需求）
        sysMenuMapper.updateStatusById(id, newVisibleStatus);

        return R.ok("更改状态成功");
    }

    //获取当前节点的子节点 （递归方法）
    private List<MenuTreeVO> buildMenuTreeRecursive(String parentId, Map<String, List<MenuTreeVO>> nodeMap) {
        //1.从map中获取当前id的所有子节点
        List<MenuTreeVO> children = nodeMap.get(parentId);
        if (CollectionUtil.isEmpty(children)) {
            return new ArrayList<>();
        }
        //2.不是空就排序
        children.sort(Comparator.comparing(MenuTreeVO::getOrderNum, Comparator.nullsLast(Integer::compareTo)));

        //3.递归查询当前节点的子节点
        for (MenuTreeVO child : children) {
            List<MenuTreeVO> subChildren = buildMenuTreeRecursive(child.getId(), nodeMap);
            child.setChildren(subChildren);
        }
        return children;
    }

    //MenuSaveReqDTO参数校验和清洗
    private static R preProcessMenuSaveReqDTO(MenuSaveReqDTO req) {
        if (req.getMenuType().equals(RbacConstants.TYPE_AUTH)) {
            //新增的是权限
            if (StrUtil.isBlank(req.getPerms())) {
                return R.fail("新增修改权限失败，授权标识(perms)不能为空");
            }
            //权限/按钮 不应该出现在菜单树，设置为隐藏
            req.setVisible(RbacConstants.MENU_HIDDEN);
            req.setPath("");  //路由为空
            req.setComponent(""); //组件为空
        } else if (req.getMenuType().equals(RbacConstants.TYPE_DIR)) {
            //目录
            if (StrUtil.isBlank(req.getPath())) {
                return R.fail("目录路径不能为空");
            }
            req.setParentId("0");
            req.setComponent("");
            req.setPerms("");
        } else if (req.getMenuType().equals(RbacConstants.TYPE_MENU)) {
            //菜单
            if (StrUtil.isBlank(req.getPath()) || StrUtil.isBlank(req.getComponent())) {
                return R.fail("菜单路径和组件不能为空");
            }
        } else {
            return R.fail("无该菜单类型!");
        }
        return null;
    }
}
