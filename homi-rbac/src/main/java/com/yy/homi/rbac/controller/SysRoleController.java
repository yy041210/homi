package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.AddRoleMenusReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.RolePageListReqDTO;
import com.yy.homi.rbac.domain.dto.request.RoleUpdateReqDTO;
import com.yy.homi.rbac.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Tag(name = "03.角色管理")
@Validated
@RestController
@RequestMapping("/sysrole")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    //查询所有角色(id,status,name)
    @Operation(summary = "查询所有角色id，name和status")
    @GetMapping("/listAll")
    public R listAll(){
        return sysRoleService.listAll();
    }

    //分页查询用户列表
    @Operation(summary = "分页查询角色列表")
    @PostMapping("/pageList")
    public R pageList(@RequestBody RolePageListReqDTO rolePageListReqDTO){
        return sysRoleService.pageList(rolePageListReqDTO);
    }

    //根据id获取角色信息
    @Operation(summary = "获取角色详细信息")

    @GetMapping("/getRoleInfo")
    public R getRoleInfo(@RequestParam("id")  @NotBlank(message = "角色id不能空") String id){
        return sysRoleService.getRoleInfo(id);
    }

    //新增角色
    @PostMapping("/insertRole")
    @Operation(summary = "新增角色")
    public R insertRole(@Validated @RequestBody RoleInsertReqDTO roleInsertReqDTO){
        return sysRoleService.insertRole(roleInsertReqDTO);
    }

    //修改角色
    @Operation(summary = "修改角色")
    @PostMapping("/updateRoleById")
    public R updateRole(@Validated @RequestBody RoleUpdateReqDTO roleUpdateReqDTO){
        return sysRoleService.updateRole(roleUpdateReqDTO);
    }

    //根据id删除角色
    @Operation(summary = "根据id删除角色")
    @GetMapping("/deleteRoleById")
    public R deleteRoleById(@RequestParam("id") @NotBlank(message = "角色id不能空") String id){
        return sysRoleService.deleteByRoleId(id);
    }

    //启用停用
    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "角色id不能空") String id){
        return sysRoleService.changeStatus(id);
    }

    //给角色添加菜单权限
    @Operation(summary = "给角色分配菜单权限")
    @Parameters({
            @Parameter(name = "roleId", description = "角色ID", required = true),
            @Parameter(name = "menuId", description = "菜单ID", required = true)
    })
    @PostMapping("/addRoleMenuRelation")
    public R addRoleMenuRelation(@Validated @RequestBody AddRoleMenusReqDTO addRoleMenusReqDTO){
        return sysRoleService.addRoleMenuRelation(addRoleMenusReqDTO);
    }


}
