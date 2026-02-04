package com.yy.homi.rbac.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.constant.CommonConstants;
import com.yy.homi.common.constant.RbacConstants;
import com.yy.homi.common.constant.RedisConstants;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.to.SysUserCache;
import com.yy.homi.rbac.domain.convert.SysUserConvert;
import com.yy.homi.rbac.domain.dto.request.UserInsertReqDTO;
import com.yy.homi.rbac.domain.dto.request.UserUpdateReqDTO;
import com.yy.homi.rbac.domain.entity.SysRole;
import com.yy.homi.rbac.domain.entity.SysUser;
import com.yy.homi.rbac.domain.dto.request.UserPageListResDTO;
import com.yy.homi.rbac.domain.entity.SysUserDetails;
import com.yy.homi.rbac.domain.entity.SysUserRole;
import com.yy.homi.common.enums.EncryptorEnum;
import com.yy.homi.rbac.domain.vo.SysUserVO;
import com.yy.homi.rbac.mapper.SysRoleMapper;
import com.yy.homi.rbac.mapper.SysUserMapper;
import com.yy.homi.rbac.mapper.SysUserRoleMapper;
import com.yy.homi.rbac.service.SysUserRoleService;
import com.yy.homi.rbac.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService, UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysUserRoleService sysUserRoleService;
    @Autowired
    private SysUserConvert sysUserConvert;
    @Autowired
    private RedisTemplate redisTemplate;

    //spring security加载正确的用户
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //1.获取用户信息
        SysUser sysUser = sysUserMapper.selectByUserNameNeId(username, "");
        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        //2.查询用户的角色标识和权限标识集合
        //例如：{"ROLE_ADMIN","sys:user:delete"}
        List<String> permissonList = sysUserMapper.selectUserPermissionsById(sysUser.getId());
        Set<String> permissions = new HashSet<>(permissonList);

        //3.封装UserDetails结果并返回
        SysUserDetails sysUserDetails = sysUserConvert.toSysUserDetails(sysUser);
        sysUserDetails.setPermissions(permissions);

        return sysUserDetails;
    }

    @Override
    public R pageList(UserPageListResDTO request) {
        Integer pageNum = request.getPageNum();
        Integer pageSize = request.getPageSize();
        String userName = request.getUserName();
        String phonenumber = request.getPhonenumber();
        String email = request.getEmail();
        Integer status = request.getStatus();
        Date beginTime = request.getBeginTime();
        Date endTime = request.getEndTime();

        //1.设置分页参数
        PageHelper.startPage(pageNum, pageSize);

        List<SysUser> sysUserList = sysUserMapper.selectUserList(userName, phonenumber, email, status, beginTime, endTime);
        List<SysUserVO> voList = sysUserConvert.toVoList(sysUserList);

        List<String> userIds = voList.stream().map(SysUserVO::getId).collect(Collectors.toList());
        List<SysUserRole> sysUserRoles = sysUserRoleMapper.selectByUserIds(userIds);
        if(CollectionUtil.isEmpty(sysUserRoles)){
            voList.stream().forEach(vo -> vo.setRoleIds(Collections.emptyList()));
            return R.ok(new PageInfo<>(voList));
        }
        
        //根据userId分组
        Map<String,List<String>> userRoleIdsMapByUserId = sysUserRoles.stream()
                .collect(Collectors.groupingBy(
                        SysUserRole::getUserId,
                        Collectors.mapping(SysUserRole::getRoleId,  // 提取角色ID
                                Collectors.toList())
                ));

        voList.stream().forEach(vo -> {
            vo.setRoleIds( userRoleIdsMapByUserId.get(vo.getId()) == null ? Collections.emptyList() : userRoleIdsMapByUserId.get(vo.getId()) );
        });

        return R.ok(new PageInfo<>(voList));
    }

    @Override
    public R getUserInfo(String id) {
        if (StrUtil.isEmpty(id)) {
            return R.fail("用户id不能为空！");
        }
        //1.先从redis拿
        String userCacheKey = RedisConstants.RBAC.USER_CACHE_PREFIX + id;
        SysUserCache sysUserCache = (SysUserCache) redisTemplate.opsForValue().get(userCacheKey);
        if (sysUserCache != null) {
            return R.ok(sysUserCache);
        }

        //2.没拿到就查数据库
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            return R.fail("用户不存在！");
        }
        SysUserCache result = sysUserConvert.toSysUserCache(sysUser);
        List<String> permissonList = sysUserMapper.selectUserPermissionsById(sysUser.getId());
        Set<String> permissions = new HashSet<>(permissonList);
        result.setPermissions(permissions);

        //3.存入Redis
        redisTemplate.opsForValue().set(userCacheKey, result, RedisConstants.RBAC.USER_CACHE_EXPIRE, TimeUnit.HOURS);
        return R.ok(result);
    }

    @Override
    @Transactional
    public R insertUser(UserInsertReqDTO userInsertReqDTO) {
        String userName = userInsertReqDTO.getUserName();

        SysUser sysUser = sysUserMapper.selectByUserNameNeId(userName, "");
        if (sysUser != null) {
            return R.fail("用户名已存在!");
        }
        sysUser = sysUserConvert.insertReqToEntity(userInsertReqDTO);

        //密码加密使用BCrypt
        sysUser.setPassword(EncryptorEnum.BCRYPT.encrypt(sysUser.getPassword()));

        //插入用户表
        sysUserMapper.insert(sysUser);
        String userId = sysUser.getId();
        List<String> roleIds = userInsertReqDTO.getRoleIds();
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<SysUserRole> sysUserRoles = new ArrayList<>();
            roleIds.stream().forEach(item -> {
                SysUserRole sysUserRole = new SysUserRole();
                sysUserRole.setUserId(userId);
                sysUserRole.setRoleId(item);
                sysUserRoles.add(sysUserRole);
            });
            sysUserRoleService.saveBatch(sysUserRoles);
        }
        return R.ok("success");
    }


    @Override
    @Transactional
    public R updateUserById(UserUpdateReqDTO req) {
        //查询用户是否存在
        String userId = req.getId();
        SysUser sysUserDB = sysUserMapper.selectById(userId);
        if (sysUserDB == null) {
            return R.fail("用户不存在！");
        }
        sysUserDB = sysUserMapper.selectByUserNameNeId(req.getUserName(), userId);
        if (sysUserDB != null) {
            return R.fail("改账号已存在！");
        }

        //1.转为SysUser
        SysUser sysUser = sysUserConvert.updateReqDTOToEntity(req);

        //2.修改sysUser基本信息
        sysUserMapper.updateById(sysUser);

        //3.修改用户关联的角色
        sysUserRoleMapper.deleteByUserId(userId); //先删除该用户原有的所有角色关联
        List<String> roleIds = req.getRoleIds();
        if (CollectionUtil.isNotEmpty(roleIds)) {
            List<SysUserRole> sysUserRoles = roleIds.stream().map(roleId -> {
                return new SysUserRole(userId, roleId);
            }).collect(Collectors.toList());
            //插入sys_user_role
            sysUserRoleService.saveBatch(sysUserRoles);
        }

        //4.清空缓存
        redisTemplate.delete(RedisConstants.RBAC.USER_CACHE_PREFIX + userId);

        return R.ok("修改成功");
    }

    @Override
    @Transactional
    public R deleteUserById(String userId) {
        if (StrUtil.isEmpty(userId)) {
            return R.fail("用户id不能为空！");
        }
        //不能删除管理员
        if (userId.equals(RbacConstants.ADMIN_ROLE_ID)) {
            return R.fail("超级管理员账号禁止删除!");
        }

        //1.删除用户关联角色信息
        sysUserRoleMapper.deleteByUserId(userId);

        //2.删除用户基本信息
        sysUserMapper.deleteById(userId);

        //3.清空缓存
        redisTemplate.delete(RedisConstants.RBAC.USER_CACHE_PREFIX + userId);

        return R.ok();
    }

    @Override
    @Transactional
    public R deleteUsers(List<String> userIds) {
        if (CollectionUtil.isEmpty(userIds)) {
            return R.ok();
        }
        // 超级管理员不能删除
        if (userIds.contains(RbacConstants.ADMIN_ROLE_ID)) {
            return R.fail("包含超级管理员账号，禁止批量删除！");
        }
        //3.批量删除关联表
        sysUserRoleMapper.deleteByUserIds(userIds);

        //4.删除用户表
        sysUserMapper.deleteBatchIds(userIds);

        //5.批量清理redis缓存
        Set<String> userCacheKeys = userIds.stream().map(userId -> {
            return RedisConstants.RBAC.USER_CACHE_PREFIX + userId;
        }).collect(Collectors.toSet());
        redisTemplate.delete(userCacheKeys);

        return R.ok();
    }

    @Override
    @Transactional
    public R changeStatus(String userId) {
        if (StrUtil.isBlank(userId)) {
            return R.fail("用户id不能为空!");
        }
        //禁止禁用管理员
        if(userId.equals(RbacConstants.ADMIN_ROLE_ID)){
            return R.fail("禁止禁用管理员！");
        }
        //1.查询用户是否存在
        SysUser sysUserDB = sysUserMapper.selectById(userId);
        if (sysUserDB == null) {
            return R.fail("用户不存在！");
        }

        //2.切换状态
        int newStatus = sysUserDB.getStatus() == CommonConstants.STATUS_ENABLED ? CommonConstants.STATUS_DISABLED : CommonConstants.STATUS_ENABLED;
        sysUserMapper.changeStatusById(userId,newStatus);

        //3.删除redis缓存
        redisTemplate.delete(RedisConstants.RBAC.USER_CACHE_PREFIX+userId);

        return R.ok();
    }

    @Override
    @Transactional
    public R addUserRoleRelation(String userId, String roleId) {
        if(StrUtil.isBlank(userId) || StrUtil.isBlank(roleId)){
            return R.fail("用户id或角色id不能为空！");
        }

        //1.检查用户是否存在
        SysUser sysUserDB = sysUserMapper.selectById(userId);
        if(sysUserDB == null){
            return R.fail("用户不存在!");
        }else if (sysUserDB.getStatus() == CommonConstants.STATUS_DISABLED){
            return R.fail("用户已被禁用！");
        }
        SysRole sysRoleDB = sysRoleMapper.selectById(roleId);
        if(sysRoleDB == null){
            return R.fail("角色不存在!");
        }else if (sysRoleDB.getStatus() == CommonConstants.STATUS_DISABLED){
            return R.fail("角色已被禁用！");
        }

        //2.幂等校验
        int count = sysUserRoleMapper.countByRelation(userId,roleId);
        if(count > 0){
            return R.fail("该用户已拥有此角色，请勿重复添加");
        }

        //3.插入数据库
        SysUserRole sysUserRole = new SysUserRole(userId, roleId);
        int rows = sysUserRoleMapper.insert(sysUserRole);
        if(rows > 0){
            //清除缓存
            redisTemplate.delete(RedisConstants.RBAC.USER_CACHE_PREFIX+userId);
            return R.ok();
        }
        return R.fail("插入失败！");
    }

}
