package com.yy.homi.rbac.domain.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@Schema(description = "角色新增请求对象")
public class RoleUpdateReqDTO {

    @Schema(description = "角色ID (修改时必传)")
    @NotBlank(message = "角色id不能空")
    private String id;

    @Schema(description = "角色名,如:管理员")
    private String roleName;

    @Schema(description = "角色权限字符串，如：admin, common")
    private String roleKey;

    @Schema(description = "显示顺序")
    private Integer roleSort;

    @Schema(description = "角色状态 (0正常 1停用)")
    private Integer status ;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "菜单组（关联的菜单ID列表）")
    private List<String> menuIds;
}