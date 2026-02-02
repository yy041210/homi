package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "菜单分页查询请求对象")
public class MenuPageListReqDTO {

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "菜单名称 (支持模糊搜索)", example = "用户管理")
    private String menuName;

    @Schema(description = "菜单类型 (M目录 C菜单 F按钮)", example = "C")
    private String menuType;

    @Schema(description = "显示状态 (0显示 1隐藏)", example = "0")
    private Integer visible;

    @Schema(description = "菜单状态 (0正常 1停用)", example = "0")
    private Integer status;
}