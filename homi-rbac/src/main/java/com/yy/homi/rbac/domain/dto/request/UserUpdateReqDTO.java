package com.yy.homi.rbac.domain.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Schema(description = "修改用户请求对象")
public class UserUpdateReqDTO {

    @Schema(description = "用户ID", required = true, example = "1")
    @NotBlank(message = "用户ID不能为空")
    private String id;

    @Schema(description = "用户账号", required = true, example = "yygogo")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 5, max = 20, message = "账号长度必须在5-20位之间")
    private String userName;

    @Schema(description = "用户昵称", required = true, example = "程序员小王")
    @NotBlank(message = "用户昵称不能为空")
    private String nickName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号码")
    private String phonenumber;

    @Schema(description = "性别 (0男 1女 2未知)", example = "0")
    private Integer sex;

    @Schema(description = "状态 (0正常 1停用)", example = "0")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    /** 关键字段：用户分配的角色ID列表 */
    @Schema(description = "角色ID列表", example = "[\"1\", \"100\"]")
    private List<String> roleIds;

}