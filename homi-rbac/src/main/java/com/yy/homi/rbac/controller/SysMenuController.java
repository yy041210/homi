package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.ConditionGetMenuTreeReqDTO;
import com.yy.homi.rbac.domain.dto.request.MenuSaveReqDTO;
import com.yy.homi.rbac.domain.dto.request.MenuPageListReqDTO;
import com.yy.homi.rbac.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Tag(name = "04.菜单权限管理")
@Validated
@RestController
@RequestMapping("/sysmenu")
public class SysMenuController {
    @Autowired
    private SysMenuService sysMenuService;


    @Operation(summary = "查询菜单权限列表")
    @PostMapping("/pageList")
    public R pageList(@RequestBody MenuPageListReqDTO menuPageListReqDTO){
        return sysMenuService.pageList(menuPageListReqDTO);
    }

    //查询所有菜单(id,visible,status,name,menuType)
    @Operation(summary = "查询所有角色id，name和status")
    @GetMapping("/listAll")
    public R listAll(){
        return sysMenuService.listAll();
    }

    //根据id获取菜单信息
    @Operation(summary = "根据id查询菜单权限信息")
    @GetMapping("/getMenuInfo")
    public R getMenuInfo(@RequestParam("id") @NotBlank(message = "菜单id不能为空!") String id){
        return sysMenuService.getMenuInfo(id);
    }

    //新增菜单
    @Operation(summary = "新增菜单权限")
    @PostMapping("/insertMenu")
    public R insertMenu(@Validated @RequestBody MenuSaveReqDTO menuSaveReqDTO){
        return sysMenuService.insertMenu(menuSaveReqDTO);
    }

    //修改菜单
    @Operation(summary = "根据id修改菜单权限信息")
    @PostMapping("/updateMenuById")
    public R updateMenu(@Validated @RequestBody MenuSaveReqDTO menuSaveReqDTO){
        return sysMenuService.updateMenu(menuSaveReqDTO);
    }

    //根据id删除角菜单
    @Operation(summary = "根据id删除菜单权限")
    @GetMapping("/deleteMenuById")
    public R deleteMenuById(@RequestParam("id") @NotBlank(message = "菜单id不能为空!") String id){
        return sysMenuService.deleteMenuById(id);
    }

    //启用停用
    @Operation(summary = "启用或停用菜单", description = "启用或停用菜单")
    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "菜单id不能为空!") String id){
        return sysMenuService.changeStatus(id);
    }

    //隐藏显示
    @Operation(summary = "显示或隐藏菜单", description = "显示或隐藏菜单")
    @GetMapping("/changeVisible")
    public R changeVisible(@RequestParam("id") @NotBlank(message = "菜单id不能为空!") String id){
        return sysMenuService.changeVisible(id);
    }

    //获取菜单树形结构
    @Operation(summary = "条件查询获取菜单权限树形结构", description = "返回层级结构的菜单，用于前端展示")
    @PostMapping("/getMenuTree")
    public R getMenuTree(@RequestBody ConditionGetMenuTreeReqDTO conditionGetMenuTreeReqDTO){
        return sysMenuService.getMenuTree(conditionGetMenuTreeReqDTO);
    }

    //根据用户id获取可见菜单树形结构
    @Operation(summary = "根据用户id获取菜单权限树形结构", description = "返回层级结构的菜单，用于前端展示")
    @GetMapping("/getMenuTreeByUserId")
    public R getMenuTreeByUserId(@RequestParam("userId") @NotBlank(message = "用户id不能为空！") String userId){
        return sysMenuService.getMenuTreeByUserId(userId);
    }

}
