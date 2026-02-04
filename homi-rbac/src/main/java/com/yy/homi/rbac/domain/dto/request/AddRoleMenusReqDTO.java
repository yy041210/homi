package com.yy.homi.rbac.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AddRoleMenusReqDTO {

    @NotBlank(message = "角色id不能为空")
    private String roleId;

    @NotEmpty(message = "绑定的菜单ids不能为空！")
    private List<String> menuIds;
}
