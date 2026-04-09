package com.yy.homi.hotel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yy.homi.common.domain.entity.R;
import com.yy.homi.hotel.domain.dto.request.SparkTaskPageListReqDTO;
import com.yy.homi.hotel.domain.entity.SparkTask;
import com.yy.homi.hotel.mapper.SparkTaskMapper;
import com.yy.homi.hotel.service.SparkTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SparkTaskServiceImpl extends ServiceImpl<SparkTaskMapper,SparkTask> implements SparkTaskService {

    @Autowired
    private SparkTaskMapper sparkTaskMapper;

    @Override
    public R pageList(SparkTaskPageListReqDTO reqDTO) {
        // 1. 开启分页
        PageHelper.startPage(reqDTO.getPageNum(), reqDTO.getPageSize());

        // 2. 执行查询，支持动态条件
        List<SparkTask> list = sparkTaskMapper.selectList(
                new LambdaQueryWrapper<SparkTask>()
                        .eq(StringUtils.hasText(reqDTO.getTaskType()), SparkTask::getTaskType, reqDTO.getTaskType())
                        .eq(reqDTO.getStatus() != null, SparkTask::getStatus, reqDTO.getStatus())
                        .orderByDesc(SparkTask::getStartTime) // 按任务开始时间倒序
        );

        // 3. 封装分页结果
        PageInfo<SparkTask> pageInfo = new PageInfo<>(list);

        // 4. 返回 R 对象
        return R.ok(pageInfo);
    }
}
