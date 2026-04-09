package com.yy.homi.rbac.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import java.util.*;

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
        wrapper.eq(reqDTO.getOperType() != null, SysLog::getBusinessType, reqDTO.getOperType());
        wrapper.orderByDesc(SysLog::getOperTime);

        List<SysLog> list = this.list(wrapper);
        // 3. 封装分页结果
        PageInfo<SysLog> pageInfo = new PageInfo<>(list);

        return R.ok(pageInfo);
    }

    @Override
    public R getStatisticLine(Long beginTime, Long endTime) {
        // 1. 将时间戳转换为 Date 对象供 SQL 使用
        Date start = new Date(beginTime);
        Date end = new Date(endTime);

        // 2. 构造 SQL：按天(Date)分组，并使用 CASE WHEN 统计不同业务类型的数量
        // 业务类型对应关系：1-新增，2-修改，3-删除，4-查询
        String selectSql = "DATE(oper_time) as operDate, " +
                "COUNT(CASE WHEN business_type = 0 THEN 1 END) as otherCount, " +
                "COUNT(CASE WHEN business_type = 1 THEN 1 END) as insertCount, " +
                "COUNT(CASE WHEN business_type = 3 THEN 1 END) as deleteCount, " +
                "COUNT(CASE WHEN business_type = 2 THEN 1 END) as updateCount, " +
                "COUNT(CASE WHEN business_type = 4 THEN 1 END) as selectCount";

        QueryWrapper<SysLog> wrapper = new QueryWrapper<>();
        wrapper.select(selectSql)
                .between("oper_time", start, end)
                .groupBy("operDate")
                .orderByAsc("operDate");

        List<Map<String, Object>> maps = this.baseMapper.selectMaps(wrapper);

        // 3. 构造前端 Echarts 所需的格式
        Map<String, Object> result = new HashMap<>();
        List<String> dateList = new ArrayList<>();
        List<Long> others = new ArrayList<>();
        List<Long> inserts = new ArrayList<>();
        List<Long> updates = new ArrayList<>();
        List<Long> deletes = new ArrayList<>();
        List<Long> selects = new ArrayList<>();

        if (maps != null) {
            for (Map<String, Object> map : maps) {
                // 添加日期轴
                dateList.add(map.get("operDate").toString());
                // 添加各类行为统计值，处理 null 情况
                others.add(getMapLongVal(map.get("otherCount")));
                inserts.add(getMapLongVal(map.get("insertCount")));
                updates.add(getMapLongVal(map.get("updateCount")));
                deletes.add(getMapLongVal(map.get("deleteCount")));
                selects.add(getMapLongVal(map.get("selectCount")));
            }
        }

        result.put("dates", dateList);
        result.put("inserts", inserts);
        result.put("updates", updates);
        result.put("deletes", deletes);
        result.put("selects", selects);
        result.put("others",others);

        return R.ok(result);
    }

    /**
     * 安全获取 Map 中的 Long 数值
     */
    private Long getMapLongVal(Object val) {
        return val == null ? 0L : ((Number) val).longValue();
    }
}
