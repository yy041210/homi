package com.yy.homi.rbac.domain.entity;

import cn.hutool.core.collection.CollectionUtil;
import com.yy.homi.common.constant.CommonConstants;
import lombok.Data;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@ToString
public class SysUserDetails implements UserDetails{
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private String id;

    /**
     * 用户账号
     */
    private String userName;

    /*密码*/
    private String password;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 帐号状态（0正常 1停用）
     */
    private Integer status;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private Integer sex;

    /**
     * 头像地址
     */
    private String avatar;

    /**
     * 备注
     */
    private String remark;

    private Set<String> permissions;


    //security需要的 权限集合转换
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (CollectionUtil.isEmpty(permissions)) {
            return Collections.emptyList();
        }
        return permissions
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    //账户是否未过期，true账户有效
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    //账户是否比未锁定,true账户有效
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    //账户 凭证是否未过期
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    //账户是否启用状态
    @Override
    public boolean isEnabled() {
        return status == CommonConstants.STATUS_ENABLED;
    }
}
