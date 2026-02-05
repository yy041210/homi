package com.yy.homi.rbac.controller;

import com.yy.homi.common.domain.entity.R;
import com.yy.homi.rbac.domain.dto.request.AddUserRolesReqDTO;
import com.yy.homi.rbac.domain.dto.request.UserInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.UserPageListResDTO;
import com.yy.homi.rbac.domain.dto.request.UserUpdateReqDTO;
import com.yy.homi.rbac.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Tag(name = "02.用户管理", description = "系统用户的增删改查、状态控制及角色分配")
@Validated
@RestController
@RequestMapping("/sysuser")
public class SysUserController {
    @Autowired
    private SysUserService sysUserService;

    //分页查询用户列表
    @Operation(summary = "分页查询用户列表")
    @PostMapping("/pageList")
    public R pageList(@RequestBody UserPageListResDTO userPageListResDTO) {
        return sysUserService.pageList(userPageListResDTO);
    }

    //根据id获取信息
    @Operation(summary = "获取用户信息")
    @Parameter(name = "id", description = "用户ID", required = true)
    @GetMapping("/getUserInfo")
    public R getUserInfo(@RequestParam("id") @NotBlank(message = "用户id不能为空") String id) {
        return sysUserService.getUserInfo(id);
    }

    //新增用户
    @Operation(summary = "新增系统用户")
    @PostMapping("/insertUser")
    public R insertUser(@Validated @RequestBody UserInsertReqDTO userInsertReqDTO) {
        return sysUserService.insertUser(userInsertReqDTO);
    }

    //修改用户
    @Operation(summary = "修改用户信息")
    @PostMapping("/updateUserById")
    public R updateUser(@Validated @RequestBody UserUpdateReqDTO userUpdateReqDTO) {
        return sysUserService.updateUserById(userUpdateReqDTO);
    }

    //根据id删除用户
    @Operation(summary = "根据ID删除用户")
    @GetMapping("/deleteUserById")
    public R deleteUserById(@RequestParam("id") @NotBlank(message = "用户id不能为空") String id) {
        return sysUserService.deleteUserById(id);
    }

    //批量删除用户
    @Operation(summary = "批量删除用户")
    @Parameter(name = "ids", description = "用户ID集合")
    @PostMapping("/deleteUsers")
    public R deleteUsers(@Validated @RequestBody @NotEmpty(message = "用户id集合不能为空！") List<String> ids) {
        return sysUserService.deleteUsers(ids);
    }

    //启用禁用
    @Operation(summary = "切换用户状态", description = "启用/禁用用户")
    @Parameter(name = "id", description = "用户ID", required = true)
    @GetMapping("/changeStatus")
    public R changeStatus(@RequestParam("id") @NotBlank(message = "用户id不能为空") String id) {
        return sysUserService.changeStatus(id);
    }

    //查询用户的已分配的角色ids
    @Operation(summary = "获取用户关联的roleIds", description = "获取用户关联的roleIds")
    @Parameter(name = "id", description = "用户ID", required = true)
    @GetMapping("/getRoleIdsByUserId")
    public R getRoleIdsByUserId(@RequestParam("id") @NotBlank(message = "用户id不能为空") String id) {
        return sysUserService.getRoleIdsByUserId(id);
    }

    //给用户分配角色
    @Operation(summary = "分配用户角色", description = "建立用户与角色的关联关系")
    @PostMapping ("/addUserRoleRelation")
    public R addUserRoleRelation(@Validated @RequestBody AddUserRolesReqDTO addUserRolesReqDTO) {
        return sysUserService.addUserRoleRelation(addUserRolesReqDTO);
    }

}
