package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.common.domain.entity.SysLog;
import com.yy.homi.rbac.domain.dto.request.SysLogPageListReqDTO;
import com.yy.homi.rbac.mapper.SysLogMapper;
import com.yy.homi.rbac.service.SysLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Autowired
    private SysLogMapper sysLogMapper;

    @Override
    public R pageList(SysLogPageListReqDTO reqDTO) {
        // 1. 开启分页
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize());

        // 2. 执行查询逻辑
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(reqDTO.getTitle()), SysLog::getTitle, reqDTO.getTitle());
        wrapper.like(StrUtil.isNotBlank(reqDTO.getOperName()), SysLog::getOperName, reqDTO.getOperName());
        wrapper.eq(reqDTO.getStatus() != null, SysLog::getStatus, reqDTO.getStatus());
        wrapper.orderByDesc(SysLog::getOperTime);

        List<SysLog> list = this.list(wrapper);
        // 3. 封装分页结果
        PageInfo<SysLog> pageInfo = new PageInfo<>(list);

        return R.ok(pageInfo);
    }
}
