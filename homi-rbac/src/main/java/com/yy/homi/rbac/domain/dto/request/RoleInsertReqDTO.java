package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Schema(description = "角色新增请求对象")
public class RoleInsertReqDTO {

    @Schema(description = "角色ID (新增不传)")
    private String id;

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 1, max = 30, message = "角色名称长度需在1-30之间")
    private String roleName;

    @NotBlank(message = "权限字符不能为空")
    @Schema(description = "角色权限字符串，如：admin, common")
    private String roleKey;

    @NotNull(message = "显示顺序不能为空")
    private Integer roleSort = 0;  //默认为零

    @Schema(description = "角色状态 (0正常 1停用)")
    private Integer status =  0;  //默认正常

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单组（关联的菜单ID列表）")
    private List<String> menuIds;
}