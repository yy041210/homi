package com.yy.homi.rbac.domain.convert;

import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.rbac.domain.dto.request.UserInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.UserUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.domain.entity.SysUserDetails;
import com.yy.homi.rbac.domain.vo.SysUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;


// componentModel = "spring" 表示生成的实现类会带上 @Component，可以直接注入
@Mapper(componentModel = "spring")
public interface SysUserConvert {
    //接口中默认属性都是静态的 隐藏修饰父 public static final ,这里是单例模式
    SysUserConvert INSTANCE = Mappers.getMapper(SysUserConvert.class);

    /**
     * 单个对象转换
     * 如果存在嵌套集合（如 List<Role> roles），MapStruct 会寻找对应的转换方法
     */
    SysUserVO toVo(SysUser entity);

    /**
     * 集合转换
     */
    List<SysUserVO> toVoList(List<SysUser> list);

    //UserInsertReqDTO 转为 SysUser
    SysUser insertReqToEntity(UserInsertReqDTO userInsertReqDTO);

    SysUserDetails toSysUserDetails(SysUser sysUser);

    SysUserCache userDetailsToUserCache(SysUserDetails SysUserDetails);
    SysUserCache toSysUserCache(SysUser sysUser);

    SysUser updateReqDTOToEntity(UserUpdateReqDTO userUpdateReqDTO);
}
