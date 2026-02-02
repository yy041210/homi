package com.yy.homi.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.yy.homi.common.constant.RbacConstants;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Date;

//自动填充字段
@Component
public class MybatisPlusHandler implements MetaObjectHandler {

    /**
     * 插入时的填充策略
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        // 参数1：实体类字段名；参数2：填充的值；参数3：元对象
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());

        fillCurrentUserId(metaObject,"createBy");
        fillCurrentUserId(metaObject,"updateBy");
    }

    /**
     * 更新时的填充策略
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
        // 如果你的任务主表里有 updateBy，也可以在这里统一填充
        fillCurrentUserId(metaObject,"updateBy");

    }

    //自动填充字段，当前用户的id，没有就是admin 1
    private void fillCurrentUserId(MetaObject metaObject,String fieldName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 判断条件：对象不为空 且 已认证 且 不是匿名用户
        String userId = RbacConstants.ADMIN_ROLE_ID; // 默认超级管理员
        if (authentication != null && authentication.isAuthenticated()) {
            String name = authentication.getName();
            // Spring Security 默认匿名用户的用户名就是 "anonymousUser"
            if (name != null && !"anonymousUser".equals(name)) {
                userId = name;
            }
        }
        this.strictInsertFill(metaObject, fieldName, String.class, userId);
    }
}