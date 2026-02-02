package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@Schema(description = "新增菜单请求对象")
public class MenuSaveReqDTO {

    @Schema(description = "菜单ID (修改时必填，新增不传)")
    private String id;

    @Schema(description = "父菜单ID (顶级菜单为0)", example = "0", required = true)
    @NotBlank(message = "上级ID不能为空")
    private String parentId;

    @Schema(description = "菜单名称", example = "用户管理", required = true)
    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 1, max = 50, message = "菜单名称长度在1-50之间")
    private String menuName;

    @Schema(description = "菜单类型 (M目录 C菜单 F按钮)", example = "C", required = true)
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    @Schema(description = "显示顺序", example = "1", required = true)
    private Integer orderNum;

    @Schema(description = "路由地址", example = "user")
    private String path;

    @Schema(description = "组件路径 (前端Vue组件路径)", example = "system/user/index")
    private String component;

    @Schema(description = "显示状态 (0显示 1隐藏)", example = "0")
    private Integer visible;

    @Schema(description = "菜单状态 (0正常 1停用)", example = "0")
    private Integer status;

    @Schema(description = "权限标识", example = "system:user:list")
    private String perms;

    @Schema(description = "菜单图标", example = "user")
    private String icon;

    @Schema(description = "备注")
    private String remark;
}