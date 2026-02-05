package com.yy.homi.rbac.domain.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AddUserRolesReqDTO {
    @NotBlank(message = "用户id不能为空")
    private String userId;

    @NotEmpty(message = "绑定的角色ids不能为空！")
    private List<String> roleIds;
}
