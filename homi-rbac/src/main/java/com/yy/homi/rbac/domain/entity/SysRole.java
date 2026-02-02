package com.yy.homi.rbac.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 角色信息表 sys_role
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 角色ID  */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 角色名称 */
    private String roleName;

    /** 角色权限字符串（如ROLE_ADMIN） */
    private String roleKey;

    /** 显示顺序 */
    private Integer roleSort;

    /** 角色状态（0正常 1停用） */
    private Integer status;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新者 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 备注 */
    private String remark;
}