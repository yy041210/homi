package com.yy.homi.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yy.homi.hotel.domain.entity.UserActionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为日志 Mapper
 */
@Mapper
public interface UserActionLogMapper extends BaseMapper<UserActionLog> {
    // 如果后续需要复杂的聚合分析（如：统计某用户的平均消费水平），可以在此处扩展 XML 映射
}