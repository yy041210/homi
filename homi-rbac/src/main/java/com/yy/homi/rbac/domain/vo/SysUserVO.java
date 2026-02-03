package com.yy.homi.rbac.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Data
public class SysUserVO implements Serializable {
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

    /** 微信统一标识 (跨端唯一) */
    private String openId;

    /** 备注 */
    private String remark;


}
