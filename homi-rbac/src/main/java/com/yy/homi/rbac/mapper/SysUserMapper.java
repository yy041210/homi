package com.yy.homi.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.rbac.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 用户表 数据层
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    
    /**
     * 根据账号查询用户
     * @param userName 用户账号
     * @return 用户信息
     */
    SysUser selectByUserNameNeId(@Param("userName") String userName,@Param("userId") String userId);

    List<SysUser> selectUserList(@Param("userName") String userName,@Param("phonenumber") String phonenumber,@Param("email") String email, @Param("status") Integer status, @Param("beginTime") Date beginTime,@Param("endTime") Date endTime);

    List<String> selectUserPermissionsById(@Param("userId") String userId);

    @Update("update sys_user set status = #{status} where id = #{userId}")
    int changeStatusById(@Param("userId") String userId,@Param("status") int status);
}