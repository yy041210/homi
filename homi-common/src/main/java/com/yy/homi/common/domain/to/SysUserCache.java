package com.yy.homi.common.domain.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class SysUserCache implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID  */
    private String id;

    /** 用户账号 */
    private String userName;

    /** 用户昵称 */
    private String nickName;

    /** 帐号状态（0正常 1停用） */
    private Integer status;

    /** 用户邮箱 */
    private String email;

    /** 手机号码 */
    private String phonenumber;

    /** 用户性别（0男 1女 2未知） */
    private Integer sex;

    /** 头像地址 */
    private String avatar;

    /** 备注 */
    private String remark;


    /**
     * 角色和权限标识集合 (如: ["ROLE_admin""sys:user:add", "sys:user:delete"])
     */
    private Set<String> permissions;

    // 提供一个辅助方法给前端，过滤出真正的权限标识（去掉 ROLE_ 前缀的）
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<String> getOnlyPermissions() {
        return permissions.stream()
                .filter(p -> !p.startsWith("ROLE_"))
                .collect(Collectors.toList());
    }
}