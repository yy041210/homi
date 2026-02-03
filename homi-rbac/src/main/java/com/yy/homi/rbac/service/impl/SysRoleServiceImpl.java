package com.yy.homi.rbac.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.convert.SysRoleConvert;
import com.yy.homi.rbac.domain.dto.request.RolePageListReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysMenu;
import com.yy.homi.rbac.domain.entity.SysRole;
import com.yy.homi.rbac.domain.vo.RoleInfoVO;
import com.yy.homi.rbac.domain.vo.RoleOptionVO;
import com.yy.homi.rbac.mapper.SysMenuMapper;
import com.yy.homi.rbac.mapper.SysRoleMapper;
import com.yy.homi.rbac.mapper.SysRoleMenuMapper;
import com.yy.homi.rbac.mapper.SysUserRoleMapper;
import com.yy.homi.rbac.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysMenuMapper sysMenuMapper;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysRoleConvert sysRoleConvert;

    @Override
    public R pageList(RolePageListReqDTO req) {
        //1.开启分页
        PageHelper.startPage(req.getPageNum(), req.getPageSize());

        //2.执行sql查询
        List<SysRole> sysRoleList = sysRoleMapper.selectRoleList(req.getRoleName(), req.getRoleKey(), req.getStatus(), req.getBeginTime(), req.getEndTime());

        //3.分装PageInfo
        PageInfo<SysRole> pageInfo = new PageInfo<>(sysRoleList);
        return R.ok(pageInfo);
    }

    @Override
    public R getRoleInfo(String roleId) {
        if (StrUtil.isBlank(roleId)) {
            return R.fail("角色id不能为空");
        }
        //1.查询角色
        SysRole sysRole = sysRoleMapper.selectById(roleId);
        if (sysRole == null) {
            return R.fail("角色不存在");
        }

        //2.查询关联的menuIds
        List<String> menuIds = sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);

        //3.组装RoleInfoVO返回
        RoleInfoVO roleInfoVO = new RoleInfoVO();
        roleInfoVO.setRole(sysRole);
        roleInfoVO.setMenuIds(menuIds);
        return R.ok(roleInfoVO);
    }

    @Override
    @Transactional
    public R insertRole(RoleInsertReqDTO req) {
        //参数校验
        SysRole sysRole = sysRoleMapper.selectOneByNameOrKey(req.getRoleName(), req.getRoleKey(), "");
        if (sysRole != null) {
            return R.fail("当前角色名或角色权限符已存在!");
        }
        //2.将req转为SysRole插入数据库
        SysRole insertSysRole = sysRoleConvert.insertReqDTOToEntity(req);
        sysRoleMapper.insert(insertSysRole);

        //3.插入角色和权限的关联关系
        List<String> menuIds = req.getMenuIds();
        if (CollectionUtil.isEmpty(menuIds)) {
            return R.ok("插入成功，没有绑定权限!");
        }
        sysRoleMenuMapper.insertBatch(insertSysRole.getId(), menuIds);
        return R.ok("插入成功");
    }


    @Override
    @Transactional
    public R updateRole(RoleUpdateReqDTO req) {
        // 1. 检查角色是否存在
        SysRole oldRole = sysRoleMapper.selectById(req.getId());
        if (oldRole == null) {
            return R.fail("修改失败，角色不存在");
        }

        //2.判断roleName或者roleKey和别的角色冲突
        SysRole sysRole = sysRoleMapper.selectOneByNameOrKey(req.getRoleName(), req.getRoleKey(), req.getId()); //这里参数roleId 是id != #{roleId}
        if (sysRole != null) {
            return R.fail("修改角色失败，角色名或角色权限标识已存在");
        }

        //3.更新主体信息
        SysRole newSysRole = sysRoleConvert.updateReqDTOToEntity(req);
        sysRoleMapper.updateById(newSysRole);

        //4.更新角色与菜单关联关系
        sysRoleMenuMapper.deleteByRoleId(req.getId());
        List<String> menuIds = req.getMenuIds();
        if (CollectionUtil.isNotEmpty(menuIds)) {
            sysRoleMenuMapper.insertBatch(req.getId(), menuIds);
        }

        return R.ok("修改角色成功");
    }


    @Override
    @Transactional
    public R deleteByRoleId(String roleId) {
        if (StrUtil.isEmpty(roleId)) {
            return R.fail("角色id不能为空");
        }

        // 1. 检查角色是否存在
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            return R.fail("角色不存在或已被删除");
        }

        // 2. 检查角色是否正在被用户使用
        // 逻辑：select count(*) from sys_user_role where role_id = ?
        int userCount = sysUserRoleMapper.countUserByRoleId(roleId);
        if (userCount > 0) {
            return R.fail("该角色已分配给用户，禁止删除");
        }

        //3.删除角色与菜单的关系
        sysRoleMapper.deleteByRoleId(roleId);

        //5. 删除角色主体 (sys_role)
        sysRoleMapper.deleteById(roleId);
        return R.ok("删除成功");
    }

    @Override
    @Transactional
    public R changeStatus(String roleId) {
        if (StrUtil.isEmpty(roleId)) {
            return R.fail("角色ID不能为空");
        }

        // 1. 查询角色当前信息
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            return R.fail("角色不存在");
        }

        // 2. 超级管理员保护：禁止禁用管理员角色
        if (RbacConstants.ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
            return R.fail("超级管理员角色禁止禁用");
        }

        // 3. 状态取反 (0正常 -> 1停用, 1停用 -> 0正常)
        // 也可以让前端传状态值，但取反逻辑更安全，防止并发误操作
        Integer newStatus = role.getStatus() == CommonConstants.STATUS_ENABLED ? CommonConstants.STATUS_DISABLED : CommonConstants.STATUS_ENABLED;

        // 4. 执行更新
        SysRole updateRole = new SysRole();
        updateRole.setId(roleId);
        updateRole.setStatus(newStatus);
        sysRoleMapper.updateById(updateRole);

        return R.ok(newStatus == 0 ? "启用成功" : "禁用成功");
    }

    @Override
    public R addRoleMenuRelation(String roleId, String menuId) {
        // 1. 业务校验：检查角色和菜单是否真实存在（防止脏数据）
        SysRole sysRole = sysRoleMapper.selectById(roleId);
        if(sysRole == null){
            return R.fail("用户不存在！");
        }
        SysMenu sysMenu = sysMenuMapper.selectById(menuId);
        if(sysMenu == null){
            return R.fail("菜单不存在");
        }

        // 2. 检查是否已经存在该关联
        int count = sysRoleMenuMapper.countByRoleIdAndMenuId(roleId, menuId);
        if (count > 0) {
            return R.fail("该角色已拥有该菜单权限，请勿重复添加");
        }

        // 3. 执行插入
        sysRoleMenuMapper.addRoleMenu(roleId, menuId);
        return R.ok("插入成功！");
    }

    @Override
    public R listAll() {
        List<SysRole> sysRoleList = this.lambdaQuery()
                .select(SysRole::getId, SysRole::getRoleName, SysRole::getStatus)
                .list();
        List<RoleOptionVO> roleOptionVOList = sysRoleConvert.toRoleOptionVOList(sysRoleList);
        return R.ok(roleOptionVOList);
    }
}
